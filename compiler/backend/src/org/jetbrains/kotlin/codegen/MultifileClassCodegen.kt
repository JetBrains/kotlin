/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.load.kotlin.incremental.IncrementalPackageFragmentProvider
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.MemberComparator
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
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

interface MultifileClassCodegen {
    fun generate(errorHandler: CompilationErrorHandler)
    fun generateClassOrObject(classOrObject: KtClassOrObject, packagePartContext: FieldOwnerContext<PackageFragmentDescriptor>)
}

class MultifileClassCodegenImpl(
        private val state: GenerationState,
        private val files: Collection<KtFile>,
        private val facadeFqName: FqName,
        private val packagePartRegistry: PackagePartRegistry
) : MultifileClassCodegen {
    private val facadeClassType = AsmUtil.asmTypeByFqNameWithoutInnerClasses(facadeFqName)

    private val packageFragment = getOnlyPackageFragment(facadeFqName.parent(), files, state.bindingContext)

    private val compiledPackageFragment = getCompiledPackageFragment(facadeFqName, state)

    private val previouslyCompiledCallables =
            if (compiledPackageFragment == null)
                emptyList<DeserializedCallableMemberDescriptor>()
            else
                getDeserializedCallables(compiledPackageFragment)

    private fun getDeserializedCallables(compiledPackageFragment: PackageFragmentDescriptor) =
            compiledPackageFragment.getMemberScope()
                    .getContributedDescriptors(DescriptorKindFilter.CALLABLES, MemberScope.ALL_NAME_FILTER)
                    .filterIsInstance<DeserializedCallableMemberDescriptor>()

    private fun KtFile.getFileClassFqName() =
            state.fileClassesProvider.getFileClassInfo(this).fileClassFqName

    private val shouldGeneratePartHierarchy =
            state.inheritMultifileParts

    private val partInternalNamesSorted = run {
        val partInternalNamesSet = hashSetOf<String>()
        for (file in files) {
            if (file.hasDeclarationsForPartClass()) {
                partInternalNamesSet.add(file.getFileClassFqName().toInternalName())
            }
        }
        compiledPackageFragment?.let {
            partInternalNamesSet.addAll(it.partsInternalNames)
        }
        partInternalNamesSet.sorted()
    }

    private val superClassForInheritedPart = run {
        val result = hashMapOf<String, String>()
        for (i in 1 ..partInternalNamesSorted.size - 1) {
            result[partInternalNamesSorted[i]] = partInternalNamesSorted[i - 1]
        }
        result
    }

    private val delegateGenerationTasks = hashMapOf<MemberDescriptor, () -> Unit>()

    private fun getSuperClassForPart(partInternalName: String) =
        if (shouldGeneratePartHierarchy)
            superClassForInheritedPart[partInternalName] ?: J_L_OBJECT
        else
            J_L_OBJECT

    private val classBuilder = ClassBuilderOnDemand {
        val originFile = files.firstOrNull()

        val actualPackageFragment = packageFragment
                                    ?: compiledPackageFragment
                                    ?: throw AssertionError("No package fragment for multifile facade $facadeFqName; files: $files")

        val declarationOrigin = MultifileClass(originFile, actualPackageFragment)

        val singleSourceFile =
                if (previouslyCompiledCallables.isEmpty())
                    files.singleOrNull { it.hasDeclarationsForPartClass() }
                else
                    null

        val superClassForFacade =
                if (shouldGeneratePartHierarchy)
                    partInternalNamesSorted.last()
                else
                    J_L_OBJECT

        state.factory.newVisitor(declarationOrigin, facadeClassType, files).apply {
            defineClass(singleSourceFile, state.classFileVersion, FACADE_CLASS_ATTRIBUTES,
                        facadeClassType.internalName, null, superClassForFacade, ArrayUtil.EMPTY_STRING_ARRAY)
            if (singleSourceFile != null) {
                visitSource(singleSourceFile.name, null)
            }

            if (shouldGeneratePartHierarchy) {
                newMethod(OtherOrigin(actualPackageFragment), Opcodes.ACC_PRIVATE, "<init>", "()V", null, null).apply {
                    visitCode()
                    visitVarInsn(Opcodes.ALOAD, 0)
                    visitMethodInsn(Opcodes.INVOKESPECIAL, superClassForFacade, "<init>", "()V", false)
                    visitInsn(Opcodes.RETURN)
                    visitMaxs(1, 1)
                    visitEnd()
                }
            }
        }
    }

    override fun generate(errorHandler: CompilationErrorHandler) {
        assert(delegateGenerationTasks.isEmpty()) { "generate() is called twice for facade class $facadeFqName" }

        generateCodeForSourceFiles(errorHandler)

        generateDelegatesToPreviouslyCompiledParts()

        if (!partInternalNamesSorted.isEmpty()) {
            generateMultifileFacadeClass()
        }

        done()
    }

    private fun generateCodeForSourceFiles(errorHandler: CompilationErrorHandler) {
        for (file in files) {
            ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
            try {
                generatePart(file)
                state.afterIndependentPart()
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

    private fun generateMultifileFacadeClass() {
        for (member in delegateGenerationTasks.keys.sortedWith(MemberComparator.INSTANCE)) {
            delegateGenerationTasks[member]!!()
        }

        writeKotlinMultifileFacadeAnnotationIfNeeded()
    }

    override fun generateClassOrObject(classOrObject: KtClassOrObject, packagePartContext: FieldOwnerContext<PackageFragmentDescriptor>) {
        MemberCodegen.genClassOrObject(packagePartContext, classOrObject, state, null)
    }

    private fun generatePart(file: KtFile) {
        val packageFragment = this.packageFragment
                              ?: throw AssertionError("File part $file of $facadeFqName: no package fragment")

        val partType = file.getFileClassFqName().toAsmType()
        val partContext = state.rootContext.intoMultifileClassPart(packageFragment, facadeClassType, partType, file)

        generateNonPartClassDeclarations(file, partContext)

        if (!state.generateDeclaredClassFilter.shouldGeneratePackagePart(file) || !file.hasDeclarationsForPartClass()) return

        packagePartRegistry.addPart(partType.internalName.substringAfterLast('/'), facadeClassType.internalName.substringAfterLast('/'))

        val builder = state.factory.newVisitor(MultifileClassPart(file, packageFragment), partType, file)

        MultifileClassPartCodegen(
                builder, file, packageFragment,
                getSuperClassForPart(partType.internalName),
                shouldGeneratePartHierarchy,
                partContext, state
        ).generate()

        addDelegateGenerationTasksForDeclarationsInFile(file, packageFragment, partType)
    }

    private fun generateNonPartClassDeclarations(file: KtFile, partContext: FieldOwnerContext<PackageFragmentDescriptor>) {
        for (declaration in file.declarations) {
            when (declaration) {
                is KtClassOrObject ->
                    if (state.generateDeclaredClassFilter.shouldGenerateClass(declaration)) {
                        generateClassOrObject(declaration, partContext)
                    }
                is KtScript ->
                    if (state.generateDeclaredClassFilter.shouldGenerateScript(declaration)) {
                        ScriptCodegen.createScriptCodegen(declaration, state, partContext).generate()
                    }
            }
        }
    }

    private fun addDelegateGenerationTasksForDeclarationsInFile(file: KtFile, packageFragment: PackageFragmentDescriptor, partType: Type) {
        val facadeContext = state.rootContext.intoMultifileClass(packageFragment, facadeClassType, partType)
        val memberCodegen = createCodegenForDelegatesInMultifileFacade(facadeContext)
        for (declaration in file.declarations) {
            if (declaration is KtNamedFunction || declaration is KtProperty || declaration is KtTypeAlias) {
                val descriptor = state.bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration)
                if (descriptor !is MemberDescriptor) {
                    throw AssertionError("Expected callable member, was " + descriptor + " for " + declaration.text)
                }
                addDelegateGenerationTaskIfNeeded(descriptor, { memberCodegen.genSimpleMember(declaration) })
            }
        }
    }

    private fun shouldGenerateInFacade(descriptor: MemberDescriptor): Boolean {
        if (Visibilities.isPrivate(descriptor.visibility)) return false
        if (AsmUtil.getVisibilityAccessFlag(descriptor) == Opcodes.ACC_PRIVATE) return false

        if (!state.classBuilderMode.generateBodies) return true

        if (shouldGeneratePartHierarchy) {
            if (descriptor !is PropertyDescriptor || !descriptor.isConst) return false
        }

        return true
    }

    private fun addDelegateGenerationTaskIfNeeded(callable: MemberDescriptor, task: () -> Unit) {
        if (shouldGenerateInFacade(callable)) {
            delegateGenerationTasks[callable] = task
        }
    }

    private fun generateDelegatesToPreviouslyCompiledParts() {
        if (compiledPackageFragment == null) return

        for (callable in previouslyCompiledCallables) {
            val partFqName = JvmFileClassUtil.getPartFqNameForDeserialized(callable)
            val partType = AsmUtil.asmTypeByFqNameWithoutInnerClasses(partFqName)

            addDelegateGenerationTaskIfNeeded(callable, { generateDelegateToCompiledMember(callable, compiledPackageFragment, partType) })
        }
    }

    private fun generateDelegateToCompiledMember(
            member: CallableMemberDescriptor,
            compiledPackageFragment: PackageFragmentDescriptor,
            partType: Type
    ) {
        val context = state.rootContext.intoMultifileClass(compiledPackageFragment, facadeClassType, partType)

        val memberCodegen = createCodegenForDelegatesInMultifileFacade(context)

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

    private fun writeKotlinMultifileFacadeAnnotationIfNeeded() {
        if (!state.classBuilderMode.generateMetadata) return
        if (files.any { it.isScript }) return

        val extraFlags = if (shouldGeneratePartHierarchy) JvmAnnotationNames.METADATA_MULTIFILE_PARTS_INHERIT_FLAG else 0

        writeKotlinMetadata(classBuilder, state, KotlinClassHeader.Kind.MULTIFILE_CLASS, extraFlags) { av ->
            val arv = av.visitArray(JvmAnnotationNames.METADATA_DATA_FIELD_NAME)
            for (internalName in partInternalNamesSorted) {
                arv.visit(null, internalName)
            }
            arv.visitEnd()
        }
    }

    private fun createCodegenForDelegatesInMultifileFacade(facadeContext: FieldOwnerContext<*>): MemberCodegen<KtFile> =
            object : MemberCodegen<KtFile>(state, null, facadeContext, null, classBuilder) {
                override fun generateDeclaration() = throw UnsupportedOperationException()
                override fun generateBody() = throw UnsupportedOperationException()
                override fun generateKotlinMetadataAnnotation() = throw UnsupportedOperationException()
            }

    private fun done() {
        classBuilder.done()
        if (classBuilder.isComputed) {
            state.afterIndependentPart()
        }
    }

    companion object {
        private val J_L_OBJECT = AsmTypes.OBJECT_TYPE.internalName
        private val FACADE_CLASS_ATTRIBUTES = Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL or Opcodes.ACC_SUPER

        private fun getOnlyPackageFragment(packageFqName: FqName, files: Collection<KtFile>, bindingContext: BindingContext): PackageFragmentDescriptor? {
            val fragments = SmartList<PackageFragmentDescriptor>()
            for (file in files) {
                val fragment = bindingContext.get(BindingContext.FILE_TO_PACKAGE_FRAGMENT, file)
                               ?: throw AssertionError("package fragment is null for " + file + "\n" + file.text)

                assert(packageFqName == fragment.fqName) { "expected package fq name: " + packageFqName + ", actual: " + fragment.fqName }

                if (!fragments.contains(fragment)) {
                    fragments.add(fragment)
                }
            }
            if (fragments.size > 1) {
                throw IllegalStateException("More than one package fragment, files: $files | fragments: $fragments")
            }
            return fragments.firstOrNull()
        }

        private fun KtFile.hasDeclarationsForPartClass() =
                declarations.any { it is KtProperty || it is KtFunction }

        private fun FqName.toInternalName() =
                AsmUtil.internalNameByFqNameWithoutInnerClasses(this)

        private fun FqName.toAsmType() =
                AsmUtil.asmTypeByFqNameWithoutInnerClasses(this)

        private fun getCompiledPackageFragment(
                facadeFqName: FqName, state: GenerationState
        ): IncrementalPackageFragmentProvider.IncrementalMultifileClassPackageFragment? {
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