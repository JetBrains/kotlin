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
import org.jetbrains.kotlin.codegen.context.MethodContext
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.kotlin.PackageParts
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.load.kotlin.incremental.IncrementalPackageFragmentProvider
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.MemberComparator
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.MultifileClass
import org.jetbrains.kotlin.resolve.jvm.diagnostics.MultifileClassPart
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOrigin
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import java.util.*

class MultifileClassCodegen(
        private val state: GenerationState,
        val files: Collection<KtFile>,
        private val facadeFqName: FqName,
        private val packagePartRegistry: PackagePartRegistry
) {
    private val facadeClassType = AsmUtil.asmTypeByFqNameWithoutInnerClasses(facadeFqName)

    private val packageFragment = getOnlyPackageFragment(facadeFqName.parent(), files, state.bindingContext)

    private val compiledPackageFragment = getCompiledPackageFragment(facadeFqName, state)

    private val previouslyCompiledCallables =
            if (compiledPackageFragment == null)
                emptyList<DeserializedCallableMemberDescriptor>()
            else
                getDeserializedCallables(compiledPackageFragment)

    private fun getDeserializedCallables(compiledPackageFragment: PackageFragmentDescriptor) =
            compiledPackageFragment.getMemberScope().getContributedDescriptors(DescriptorKindFilter.CALLABLES, MemberScope.ALL_NAME_FILTER).filterIsInstance<DeserializedCallableMemberDescriptor>()

    private val classBuilder = ClassBuilderOnDemand {
        val originFile = files.firstOrNull()
        val actualPackageFragment = packageFragment ?:
                                    compiledPackageFragment ?:
                                    throw AssertionError("No package fragment for multifile facade $facadeFqName; files: $files")
        val declarationOrigin = MultifileClass(originFile, actualPackageFragment, facadeFqName)
        val classBuilder = state.factory.newVisitor(declarationOrigin, facadeClassType, files)

        val filesWithCallables = files.filter { it.declarations.any { it is KtNamedFunction || it is KtProperty } }

        val singleSourceFile = if (previouslyCompiledCallables.isNotEmpty()) null else filesWithCallables.singleOrNull()

        classBuilder.defineClass(singleSourceFile, Opcodes.V1_6,
                                 if (state.generateOpenMultifileClasses) OPEN_FACADE_CLASS_ATTRIBUTES else FACADE_CLASS_ATTRIBUTES,
                                 facadeClassType.internalName,
                                 null, "java/lang/Object", ArrayUtil.EMPTY_STRING_ARRAY)
        if (singleSourceFile != null) {
            classBuilder.visitSource(singleSourceFile.name, null)
        }

        if (state.generateOpenMultifileClasses) {
            generateMultifileFacadeClassConstructor(classBuilder, originFile)
        }

        classBuilder
    }

    fun generate(errorHandler: CompilationErrorHandler) {
        val generateCallableMemberTasks = HashMap<CallableMemberDescriptor, () -> Unit>()
        val partFqNames = arrayListOf<FqName>()

        generateCodeForSourceFiles(errorHandler, generateCallableMemberTasks, partFqNames)

        generateDelegatesToPreviouslyCompiledParts(generateCallableMemberTasks, partFqNames)

        if (!partFqNames.isEmpty()) {
            generateMultifileFacadeClass(generateCallableMemberTasks, partFqNames)
        }

        done()
    }

    private fun generateCodeForSourceFiles(
            errorHandler: CompilationErrorHandler,
            generateCallableMemberTasks: MutableMap<CallableMemberDescriptor, () -> Unit>,
            partFqNames: MutableList<FqName>
    ) {
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
    }

    private fun generateMultifileFacadeClass(
            tasks: Map<CallableMemberDescriptor, () -> Unit>,
            partFqNames: List<FqName>
    ) {
        for (member in tasks.keys.sortedWith(MemberComparator.INSTANCE)) {
            tasks[member]!!()
        }

        writeKotlinMultifileFacadeAnnotationIfNeeded(partFqNames)
    }

    fun generateClassOrObject(classOrObject: KtClassOrObject, packagePartContext: FieldOwnerContext<PackageFragmentDescriptor>) {
        MemberCodegen.genClassOrObject(packagePartContext, classOrObject, state, null)
    }


    private fun generateMultifileFacadeClassConstructor(classBuilder: ClassBuilder, originFile: KtFile?) {
        val mv = classBuilder.newMethod(JvmDeclarationOrigin.NO_ORIGIN, Opcodes.ACC_PUBLIC, "<init>", "()V", null, null)
        val v = InstructionAdapter(mv)
        v.load(0, facadeClassType)
        v.invokespecial("java/lang/Object", "<init>", "()V", false)
        v.visitInsn(Opcodes.RETURN)
        FunctionCodegen.endVisit(v, "multifile class constructor", originFile)
    }

    private fun generatePart(
            file: KtFile,
            generateCallableMemberTasks: MutableMap<CallableMemberDescriptor, () -> Unit>,
            partFqNames: MutableList<FqName>
    ) {
        val packageFragment = this.packageFragment ?:
                              throw AssertionError("File part $file of $facadeFqName: no package fragment")

        var generatePart = false
        val partClassInfo = state.fileClassesProvider.getFileClassInfo(file)
        val partType = AsmUtil.asmTypeByFqNameWithoutInnerClasses(partClassInfo.fileClassFqName)
        val partContext = state.rootContext.intoMultifileClassPart(packageFragment, facadeClassType, partType, file)

        for (declaration in file.declarations) {
            when (declaration) {
                is KtProperty, is KtNamedFunction -> {
                    generatePart = true
                }
                is KtClassOrObject -> if (state.generateDeclaredClassFilter.shouldGenerateClass(declaration)) {
                    generateClassOrObject(declaration, partContext)
                }
                is KtScript -> if (state.generateDeclaredClassFilter.shouldGenerateScript(declaration)) {
                    ScriptCodegen.createScriptCodegen(declaration, state, partContext).generate()
                }
            }
        }


        if (!generatePart || !state.generateDeclaredClassFilter.shouldGeneratePackagePart(file)) return

        partFqNames.add(partClassInfo.fileClassFqName)

        val name = partType.internalName
        packagePartRegistry.addPart(name.substring(name.lastIndexOf('/') + 1))

        val builder = state.factory.newVisitor(MultifileClassPart(file, packageFragment, facadeFqName), partType, file)

        MultifileClassPartCodegen(builder, file, partType, facadeClassType, partContext, state).generate()

        val facadeContext = state.rootContext.intoMultifileClass(packageFragment, facadeClassType, partType)
        val memberCodegen = createCodegenForPartOfMultifileFacade(facadeContext)
        for (declaration in file.declarations) {
            if (declaration is KtNamedFunction || declaration is KtProperty) {
                val descriptor = state.bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration)
                assert(descriptor is CallableMemberDescriptor) { "Expected callable member, was " + descriptor + " for " + declaration.text }
                if (!Visibilities.isPrivate((descriptor as CallableMemberDescriptor).visibility)
                        && AsmUtil.getVisibilityAccessFlag(descriptor) != Opcodes.ACC_PRIVATE) {
                    generateCallableMemberTasks.put(descriptor, { memberCodegen.genFunctionOrProperty(declaration) })
                }
            }
        }
    }

    private fun generateDelegatesToPreviouslyCompiledParts(
            generateCallableMemberTasks: MutableMap<CallableMemberDescriptor, () -> Unit>,
            partFqNames: MutableList<FqName>
    ) {
        if (compiledPackageFragment == null) return

        partFqNames.addAll(compiledPackageFragment.partsNames.map { JvmClassName.byInternalName(it).fqNameForClassNameWithoutDollars })

        for (callable in previouslyCompiledCallables) {
            val partFqName = JvmFileClassUtil.getPartFqNameForDeserializedCallable(callable)
            val partType = AsmUtil.asmTypeByFqNameWithoutInnerClasses(partFqName)

            generateCallableMemberTasks[callable] = { generateDelegateToCompiledMember(callable, compiledPackageFragment, partType) }
        }
    }

    private fun generateDelegateToCompiledMember(
            member: CallableMemberDescriptor,
            compiledPackageFragment: PackageFragmentDescriptor,
            partType: Type
    ) {
        val context = state.rootContext.intoMultifileClass(compiledPackageFragment, facadeClassType, partType)

        val memberCodegen = createCodegenForPartOfMultifileFacade(context)

        when (member) {
            is DeserializedSimpleFunctionDescriptor -> {
                memberCodegen.functionCodegen.generateMethod(OtherOrigin(member), member, DelegateToCompiledMemberGenerationStrategy)

                memberCodegen.functionCodegen.generateDefaultIfNeeded(
                        context.intoFunction(member), member, OwnerKind.PACKAGE, DefaultParameterValueLoader.DEFAULT, null)

                memberCodegen.functionCodegen.generateOverloadsWithDefaultValues(null, member, member)
            }
            is DeserializedPropertyDescriptor -> {
                memberCodegen.propertyCodegen.generateInPackageFacade(member)
            }
            else -> {
                throw IllegalStateException("Unexpected member: " + member)
            }
        }
    }

    object DelegateToCompiledMemberGenerationStrategy : FunctionGenerationStrategy() {
        override fun generateBody(mv: MethodVisitor, frameMap: FrameMap, signature: JvmMethodSignature, context: MethodContext, parentCodegen: MemberCodegen<*>) {
            throw IllegalStateException("shouldn't be called")
        }
    }

    private fun writeKotlinMultifileFacadeAnnotationIfNeeded(partFqNames: List<FqName>) {
        if (state.classBuilderMode != ClassBuilderMode.FULL) return
        if (files.any { it.isScript }) return

        writeKotlinMetadata(classBuilder, KotlinClassHeader.Kind.MULTIFILE_CLASS) { av ->
            val arv = av.visitArray(JvmAnnotationNames.METADATA_DATA_FIELD_NAME)
            for (internalName in partFqNames.map(AsmUtil::internalNameByFqNameWithoutInnerClasses).sorted()) {
                arv.visit(null, internalName)
            }
            arv.visitEnd()
        }
    }

    private fun createCodegenForPartOfMultifileFacade(facadeContext: FieldOwnerContext<*>): MemberCodegen<KtFile> =
            object : MemberCodegen<KtFile>(state, null, facadeContext, null, classBuilder) {
                override fun generateDeclaration() = throw UnsupportedOperationException()
                override fun generateBody() = throw UnsupportedOperationException()
                override fun generateKotlinMetadataAnnotation() = throw UnsupportedOperationException()
            }

    private fun done() {
        classBuilder.done()
    }

    companion object {
        private val FACADE_CLASS_ATTRIBUTES = Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL or Opcodes.ACC_SUPER
        private val OPEN_FACADE_CLASS_ATTRIBUTES = Opcodes.ACC_PUBLIC or Opcodes.ACC_SUPER

        private fun getOnlyPackageFragment(packageFqName: FqName, files: Collection<KtFile>, bindingContext: BindingContext): PackageFragmentDescriptor? {
            val fragments = SmartList<PackageFragmentDescriptor>()
            for (file in files) {
                val fragment = bindingContext.get(BindingContext.FILE_TO_PACKAGE_FRAGMENT, file)
                assert(fragment != null) { "package fragment is null for " + file + "\n" + file.text }

                assert(packageFqName == fragment!!.fqName) { "expected package fq name: " + packageFqName + ", actual: " + fragment.fqName }

                if (!fragments.contains(fragment)) {
                    fragments.add(fragment)
                }
            }
            if (fragments.size > 1) {
                throw IllegalStateException("More than one package fragment, files: $files | fragments: $fragments")
            }
            return fragments.firstOrNull()
        }

        private fun getCompiledPackageFragment(facadeFqName: FqName, state: GenerationState):
                IncrementalPackageFragmentProvider.IncrementalPackageFragment.IncrementalMultifileClassPackageFragment? {
            if (!IncrementalCompilation.isEnabled()) return null

            val packageFqName = facadeFqName.parent()

            val incrementalPackageFragment = state.module.getPackage(packageFqName).fragments.firstOrNull { fragment ->
                    fragment is IncrementalPackageFragmentProvider.IncrementalPackageFragment &&
                    fragment.target == state.targetId
                } as IncrementalPackageFragmentProvider.IncrementalPackageFragment?

            return incrementalPackageFragment?.getPackageFragmentForMultifileClass(facadeFqName)
        }
    }

}