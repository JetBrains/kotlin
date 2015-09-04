/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.codegen

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.util.ArrayUtil
import com.intellij.util.SmartList
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.codegen.context.CodegenContext
import org.jetbrains.kotlin.codegen.context.FieldOwnerContext
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.kotlin.incremental.IncrementalPackageFragmentProvider
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.MemberComparator
import org.jetbrains.kotlin.resolve.jvm.diagnostics.MultifileClass
import org.jetbrains.kotlin.resolve.jvm.diagnostics.MultifileClassPart
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.kotlin.serialization.PackageData
import org.jetbrains.kotlin.serialization.SerializationUtil
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.jvm.BitEncoding
import org.jetbrains.org.objectweb.asm.Opcodes
import java.util.*


public class MultifileClassCodegen(
        private val state: GenerationState,
        private val files: Collection<JetFile>,
        private val facadeFqName: FqName
) {
    private val facadeClassType = AsmUtil.asmTypeByFqNameWithoutInnerClasses(facadeFqName)

    private val packageFragment = getOnlyPackageFragment(facadeFqName.parent(), files, state.bindingContext)

    private val compiledPackageFragment = getCompiledPackageFragment(facadeFqName.parent(), state)

    // TODO incremental compilation support
    // TODO previouslyCompiledCallables
    // We can do this (probably without 'compiledPackageFragment') after modifications to part codegen.

    private val classBuilder = ClassBuilderOnDemand {
        val filesWithTopLevelCallables = files.filter { hasTopLevelCallables(it) }
        val sourceFile = filesWithTopLevelCallables.firstOrNull()
        val actualPackageFragment = packageFragment ?:
                                    compiledPackageFragment ?:
                                    throw AssertionError("No package fragment for multifile facade $facadeFqName; files: $files")
        val declarationOrigin = MultifileClass(actualPackageFragment, facadeFqName)
        val classBuilder = state.factory.newVisitor(declarationOrigin, facadeClassType, filesWithTopLevelCallables)

        classBuilder.defineClass(sourceFile, Opcodes.V1_6, FACADE_CLASS_ATTRIBUTES,
                                 facadeClassType.internalName,
                                 null, "java/lang/Object", ArrayUtil.EMPTY_STRING_ARRAY)
        sourceFile?.let { classBuilder.visitSource(it.name, null) }
        classBuilder
    }

    private fun hasTopLevelCallables(file: JetFile) =
            file.declarations.any { it is JetNamedFunction || it is JetProperty }

    public fun generate(errorHandler: CompilationErrorHandler) {
        val bindings = ArrayList<JvmSerializationBindings>(files.size() + 1)
        val generateCallableMemberTasks = HashMap<CallableMemberDescriptor, () -> Unit>()
        val partFqNames = arrayListOf<FqName>()

        for (file in files) {
            ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
            try {
                val partClassBuilder = generatePart(file, generateCallableMemberTasks, partFqNames)
                if (partClassBuilder != null) {
                    bindings.add(partClassBuilder.serializationBindings)
                }
            }
            catch (e: ProcessCanceledException) {
                throw e
            }
            catch (e: Throwable) {
                val vFile = file.virtualFile
                errorHandler.reportException(e, if (vFile == null) "no file" else vFile.url)
                DiagnosticUtils.throwIfRunningOnServer(e)
                if (ApplicationManager.getApplication().isInternal) {
                    //noinspection CallToPrintStackTrace
                    e.printStackTrace()
                }
            }
        }

//        generateDelegationsToPreviouslyCompiled(generateCallableMemberTasks)

        if (!generateCallableMemberTasks.isEmpty()) {
            generateMultifileFacadeClass(generateCallableMemberTasks, bindings, partFqNames)
        }
    }

    private fun generateMultifileFacadeClass(
            tasks: Map<CallableMemberDescriptor, () -> Unit>,
            bindings: MutableList<JvmSerializationBindings>,
            partFqNames: List<FqName>
    ) {
        MemberCodegen.generateModuleNameField(state, classBuilder)

        for (member in tasks.keySet().sortedWith(MemberComparator.INSTANCE)) {
            tasks[member]!!()
        }

        bindings.add(classBuilder.serializationBindings)
        writeKotlinMultifileFacadeAnnotationIfNeeded(JvmSerializationBindings.union(bindings), partFqNames)
    }

    public fun generateClassOrObject(classOrObject: JetClassOrObject) {
        val file = classOrObject.getContainingJetFile()
        val packagePartType = state.fileClassesProvider.getFileClassType(file)
        val context = CodegenContext.STATIC.intoPackagePart(packageFragment!!, packagePartType)
        MemberCodegen.genClassOrObject(context, classOrObject, state, null)
    }

    private fun generatePart(
            file: JetFile,
            generateCallableMemberTasks: MutableMap<CallableMemberDescriptor, () -> Unit>,
            partFqNames: MutableList<FqName>
    ): ClassBuilder? {
        val packageFragment = this.packageFragment ?:
                              throw AssertionError("File part $file of $facadeFqName: no package fragment")

        var generatePart = false
        val partClassInfo = state.fileClassesProvider.getFileClassInfo(file)
        val partType = AsmUtil.asmTypeByFqNameWithoutInnerClasses(partClassInfo.fileClassFqName)
        val partContext = CodegenContext.STATIC.intoMultifileClassPart(packageFragment, facadeClassType, partType)

        for (declaration in file.declarations) {
            if (declaration is JetProperty || declaration is JetNamedFunction) {
                generatePart = true
            }
            else if (declaration is JetClassOrObject) {
                if (state.generateDeclaredClassFilter.shouldGenerateClass(declaration)) {
                    generateClassOrObject(declaration)
                }
            }
            else if (declaration is JetScript) {

                // SCRIPT: generate script code, should be separate execution branch
                if (state.generateDeclaredClassFilter.shouldGenerateScript(declaration)) {
                    ScriptCodegen.createScriptCodegen(declaration, state, partContext).generate()
                }
            }
        }


        if (!generatePart || !state.generateDeclaredClassFilter.shouldGeneratePackagePart(file)) return null

        partFqNames.add(partClassInfo.fileClassFqName)

//        val name = partType.internalName
//        packageParts.parts.add(name.substring(name.lastIndexOf('/') + 1))

        val builder = state.factory.newVisitor(MultifileClassPart(file, packageFragment, facadeFqName), partType, file)

        MultifileClassPartCodegen(builder, file, partType, facadeFqName, partContext, state).generate()

        val facadeContext = CodegenContext.STATIC.intoMultifileClass(packageFragment, facadeClassType, partType)
        val memberCodegen = createCodegenForPartOfMultifileFacade(facadeContext)
        for (declaration in file.declarations) {
            if (declaration is JetNamedFunction || declaration is JetProperty) {
                val descriptor = state.bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration)
                assert(descriptor is CallableMemberDescriptor) { "Expected callable member, was " + descriptor + " for " + declaration.text }
                generateCallableMemberTasks.put(descriptor as CallableMemberDescriptor,
                                                { memberCodegen.genFunctionOrProperty(declaration) })
            }
        }

        return builder
    }

    private fun writeKotlinMultifileFacadeAnnotationIfNeeded(bindings: JvmSerializationBindings, partFqNames: List<FqName>) {
        if (state.classBuilderMode != ClassBuilderMode.FULL) return
        if (files.any { it.isScript }) return

        val serializer = DescriptorSerializer.createTopLevel(JvmSerializerExtension(bindings, state.typeMapper))

        val packageFragments = arrayListOf<PackageFragmentDescriptor>()
        ContainerUtil.addIfNotNull(packageFragments, packageFragment)
        ContainerUtil.addIfNotNull(packageFragments, compiledPackageFragment)
        val facadeProto = serializer.packageProto(packageFragments, /* skip= */ { true }).build()

        val strings = serializer.stringTable
        val nameResolver = NameResolver(strings.serializeSimpleNames(), strings.serializeQualifiedNames())
        val facadeData = PackageData(nameResolver, facadeProto)

        val av = classBuilder.newAnnotation(AsmUtil.asmDescByFqNameWithoutInnerClasses(JvmAnnotationNames.KOTLIN_MULTIFILE_CLASS), true)
        av.visit(JvmAnnotationNames.ABI_VERSION_FIELD_NAME, JvmAbi.VERSION)

        val shortNames = partFqNames.map { it.shortName().asString() }.sorted()
        val filePartClassNamesArray = av.visitArray(JvmAnnotationNames.FILE_PART_CLASS_NAMES_FIELD_NAME)
        for (shortName in shortNames) {
            filePartClassNamesArray.visit(null, shortName)
        }
        filePartClassNamesArray.visitEnd()

        val dataArray = av.visitArray(JvmAnnotationNames.DATA_FIELD_NAME)
        for (string in BitEncoding.encodeBytes(SerializationUtil.serializePackageData(facadeData))) {
            dataArray.visit(null, string)
        }
        dataArray.visitEnd()

        av.visitEnd()
    }

    private fun createCodegenForPartOfMultifileFacade(facadeContext: FieldOwnerContext<DeclarationDescriptor>): MemberCodegen<JetFile> =
            object : MemberCodegen<JetFile>(state, null, facadeContext, null, classBuilder) {
                override fun generateDeclaration() = throw UnsupportedOperationException()
                override fun generateBody() = throw UnsupportedOperationException()
                override fun generateKotlinAnnotation() = throw UnsupportedOperationException()
            }

    public fun done() {
        classBuilder.done()
    }

    companion object {
        private val FACADE_CLASS_ATTRIBUTES = Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL

        private fun getOnlyPackageFragment(packageFqName: FqName, files: Collection<JetFile>, bindingContext: BindingContext): PackageFragmentDescriptor? {
            val fragments = SmartList<PackageFragmentDescriptor>()
            for (file in files) {
                val fragment = bindingContext.get(BindingContext.FILE_TO_PACKAGE_FRAGMENT, file)
                assert(fragment != null) { "package fragment is null for " + file + "\n" + file.text }

                assert(packageFqName == fragment!!.fqName) { "expected package fq name: " + packageFqName + ", actual: " + fragment.fqName }

                if (!fragments.contains(fragment)) {
                    fragments.add(fragment)
                }
            }
            if (fragments.size() > 1) {
                throw IllegalStateException("More than one package fragment, files: $files | fragments: $fragments")
            }
            return fragments.firstOrNull()
        }

        private fun getCompiledPackageFragment(packageFqName: FqName, state: GenerationState): PackageFragmentDescriptor? =
                if (!IncrementalCompilation.ENABLED) null
                else state.module.getPackage(packageFqName).fragments.firstOrNull { fragment ->
                    fragment is IncrementalPackageFragmentProvider.IncrementalPackageFragment &&
                    fragment.moduleId == state.moduleId
                }
    }

}