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
import org.jetbrains.kotlin.codegen.context.FieldOwnerContext
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import org.jetbrains.kotlin.fileClasses.getFileClassType
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.kotlin.PackageParts
import org.jetbrains.kotlin.load.kotlin.incremental.IncrementalPackageFragmentProvider
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.MemberComparator
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.MultifileClass
import org.jetbrains.kotlin.resolve.jvm.diagnostics.MultifileClassPart
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import java.util.*


public class MultifileClassCodegen(
        private val state: GenerationState,
        public val files: Collection<JetFile>,
        private val facadeFqName: FqName
) {
    private val facadeClassType = AsmUtil.asmTypeByFqNameWithoutInnerClasses(facadeFqName)

    private val packageFragment = getOnlyPackageFragment(facadeFqName.parent(), files, state.bindingContext)

    private val compiledPackageFragment = getCompiledPackageFragment(facadeFqName.parent(), state)

    public val packageParts = PackageParts(facadeFqName.parent().asString())

    // TODO incremental compilation support
    // TODO previouslyCompiledCallables
    // We can do this (probably without 'compiledPackageFragment') after modifications to part codegen.

    private val classBuilder = ClassBuilderOnDemand {
        val originFile = files.firstOrNull()
        val actualPackageFragment = packageFragment ?:
                                    compiledPackageFragment ?:
                                    throw AssertionError("No package fragment for multifile facade $facadeFqName; files: $files")
        val declarationOrigin = MultifileClass(originFile, actualPackageFragment, facadeFqName)
        val classBuilder = state.factory.newVisitor(declarationOrigin, facadeClassType, files)

        val filesWithCallables = files.filter { it.declarations.any { it is JetNamedFunction || it is JetProperty } }
        val singleSourceFile = filesWithCallables.singleOrNull()
        classBuilder.defineClass(singleSourceFile, Opcodes.V1_6, FACADE_CLASS_ATTRIBUTES,
                                 facadeClassType.internalName,
                                 null, "java/lang/Object", ArrayUtil.EMPTY_STRING_ARRAY)
        if (singleSourceFile != null) {
            classBuilder.visitSource(singleSourceFile.name, null)
        }
        classBuilder
    }

    public fun generate(errorHandler: CompilationErrorHandler) {
        val generateCallableMemberTasks = HashMap<CallableMemberDescriptor, () -> Unit>()
        val partFqNames = arrayListOf<FqName>()

        for (file in files) {
            ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
            try {
                generatePart(file, generateCallableMemberTasks, partFqNames)
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
            generateMultifileFacadeClass(generateCallableMemberTasks, partFqNames)
        }
    }

    private fun generateMultifileFacadeClass(
            tasks: Map<CallableMemberDescriptor, () -> Unit>,
            partFqNames: List<FqName>
    ) {
        generateKotlinPackageReflectionField()
        MemberCodegen.generateModuleNameField(state, classBuilder)

        for (member in tasks.keySet().sortedWith(MemberComparator.INSTANCE)) {
            tasks[member]!!()
        }

        writeKotlinMultifileFacadeAnnotationIfNeeded(partFqNames)
    }

    public fun generateClassOrObject(classOrObject: JetClassOrObject) {
        val file = classOrObject.getContainingJetFile()
        val partType = state.fileClassesProvider.getFileClassType(file)
        val context = state.rootContext.intoMultifileClassPart(packageFragment!!, facadeClassType, partType)
        MemberCodegen.genClassOrObject(context, classOrObject, state, null)
    }

    private fun generatePart(
            file: JetFile,
            generateCallableMemberTasks: MutableMap<CallableMemberDescriptor, () -> Unit>,
            partFqNames: MutableList<FqName>
    ) {
        val packageFragment = this.packageFragment ?:
                              throw AssertionError("File part $file of $facadeFqName: no package fragment")

        var generatePart = false
        val partClassInfo = state.fileClassesProvider.getFileClassInfo(file)
        val partType = AsmUtil.asmTypeByFqNameWithoutInnerClasses(partClassInfo.fileClassFqName)
        val partContext = state.rootContext.intoMultifileClassPart(packageFragment, facadeClassType, partType)

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


        if (!generatePart || !state.generateDeclaredClassFilter.shouldGeneratePackagePart(file)) return

        partFqNames.add(partClassInfo.fileClassFqName)

        val name = partType.internalName
        packageParts.parts.add(name.substring(name.lastIndexOf('/') + 1))

        val builder = state.factory.newVisitor(MultifileClassPart(file, packageFragment, facadeFqName), partType, file)

        MultifileClassPartCodegen(builder, file, partType, facadeFqName, partContext, state).generate()

        val facadeContext = state.rootContext.intoMultifileClass(packageFragment, facadeClassType, partType)
        val memberCodegen = createCodegenForPartOfMultifileFacade(facadeContext)
        for (declaration in file.declarations) {
            if (declaration is JetNamedFunction || declaration is JetProperty) {
                val descriptor = state.bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration)
                assert(descriptor is CallableMemberDescriptor) { "Expected callable member, was " + descriptor + " for " + declaration.text }
                generateCallableMemberTasks.put(descriptor as CallableMemberDescriptor,
                                                { memberCodegen.genFunctionOrProperty(declaration) })
            }
        }
    }

    private fun generateKotlinPackageReflectionField() {
        val mv = classBuilder.newMethod(JvmDeclarationOrigin.NO_ORIGIN, Opcodes.ACC_STATIC, "<clinit>", "()V", null, null)
        val method = AsmUtil.method("createKotlinPackage",
                                    AsmTypes.K_PACKAGE_TYPE, AsmTypes.getType(Class::class.java), AsmTypes.getType(String::class.java))
        val iv = InstructionAdapter(mv)
        MemberCodegen.generateReflectionObjectField(state, facadeClassType, classBuilder, method, JvmAbi.KOTLIN_PACKAGE_FIELD_NAME, iv)
        iv.areturn(Type.VOID_TYPE)
        FunctionCodegen.endVisit(mv, "package facade static initializer", null)
    }

    private fun writeKotlinMultifileFacadeAnnotationIfNeeded(partFqNames: List<FqName>) {
        if (state.classBuilderMode != ClassBuilderMode.FULL) return
        if (files.any { it.isScript }) return

        val av = classBuilder.newAnnotation(AsmUtil.asmDescByFqNameWithoutInnerClasses(JvmAnnotationNames.KOTLIN_MULTIFILE_CLASS), true)
        JvmCodegenUtil.writeAbiVersion(av)

        val shortNames = partFqNames.map { it.shortName().asString() }.sorted()
        val filePartClassNamesArray = av.visitArray(JvmAnnotationNames.FILE_PART_CLASS_NAMES_FIELD_NAME)
        for (shortName in shortNames) {
            filePartClassNamesArray.visit(null, shortName)
        }
        filePartClassNamesArray.visitEnd()

        av.visitEnd()
    }

    private fun createCodegenForPartOfMultifileFacade(facadeContext: FieldOwnerContext<*>): MemberCodegen<JetFile> =
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
                if (!IncrementalCompilation.isEnabled()) null
                else state.module.getPackage(packageFqName).fragments.firstOrNull { fragment ->
                    fragment is IncrementalPackageFragmentProvider.IncrementalPackageFragment &&
                    fragment.target == state.targetId
                }
    }

}