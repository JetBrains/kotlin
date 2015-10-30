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

package org.jetbrains.kotlin.asJava

import com.google.common.collect.Lists
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.ClassFileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.compiled.ClsFileImpl
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub
import com.intellij.psi.impl.java.stubs.impl.PsiJavaFileStubImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.PsiClassHolderFileStub
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.Stack
import org.jetbrains.kotlin.codegen.CompilationErrorHandler
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.fileClasses.NoResolveFileClassesProvider
import org.jetbrains.kotlin.fileClasses.getFileClassType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils.descriptorToDeclaration
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.resolve.jvm.JvmClassName

class KotlinJavaFileStubProvider<T : WithFileStubAndExtraDiagnostics> private constructor(
        private val project: Project,
        private val local: Boolean,
        private val stubGenerationStrategy: StubGenerationStrategy<T>) : CachedValueProvider<T> {

    override fun compute(): CachedValueProvider.Result<T>? {
        val packageFqName = stubGenerationStrategy.packageFqName
        val files = stubGenerationStrategy.files

        checkForBuiltIns(packageFqName, files)

        val context = stubGenerationStrategy.getContext(files)

        val javaFileStub = createJavaFileStub(packageFqName, files)
        val bindingContext: BindingContext
        val forExtraDiagnostics = BindingTraceContext()
        try {
            val stubStack = Stack<StubElement<PsiElement>>()
            stubStack.push(javaFileStub)

            val state = GenerationState(
                    project,
                    KotlinLightClassBuilderFactory(stubStack),
                    context.module,
                    context.bindingContext,
                    Lists.newArrayList(files),
                    /*disable not-null assertions*/false, false,
                    /*generateClassFilter=*/stubGenerationStrategy.generateClassFilter,
                    /*disableInline=*/false,
                    /*disableOptimization=*/false,
                    /*useTypeTableInSerializer=*/false,
                    forExtraDiagnostics)
            KotlinCodegenFacade.prepareForCompilation(state)

            bindingContext = state.bindingContext

            stubGenerationStrategy.generate(state, files)

            val pop = stubStack.pop()
            if (pop !== javaFileStub) {
                LOG.error("Unbalanced stack operations: " + pop)
            }
        }
        catch (e: ProcessCanceledException) {
            throw e
        }
        catch (e: RuntimeException) {
            logErrorWithOSInfo(e, packageFqName, null)
            throw e
        }

        val extraDiagnostics = forExtraDiagnostics.bindingContext.diagnostics
        return CachedValueProvider.Result.create(
                stubGenerationStrategy.createLightClassData(javaFileStub, bindingContext, extraDiagnostics),
                if (local) PsiModificationTracker.MODIFICATION_COUNT else PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT)
    }

    private fun createJavaFileStub(packageFqName: FqName, files: Collection<KtFile>): PsiJavaFileStub {
        val javaFileStub = PsiJavaFileStubImpl(packageFqName.asString(), true)
        javaFileStub.psiFactory = ClsWrapperStubPsiFactory()

        val fakeFile = createFakeClsFile(project, packageFqName, files, object : Function0<PsiClassHolderFileStub<PsiFile>> {
            override fun invoke(): PsiClassHolderFileStub<PsiFile> {
                return javaFileStub
            }
        })

        javaFileStub.setPsi(fakeFile)
        return javaFileStub
    }

    companion object {

        fun createForFacadeClass(
                project: Project,
                facadeFqName: FqName,
                searchScope: GlobalSearchScope): CachedValueProvider<KotlinFacadeLightClassData> {
            return KotlinJavaFileStubProvider(
                    project,
                    false,
                    object : StubGenerationStrategy<KotlinFacadeLightClassData> {
                        override val files: Collection<KtFile>
                            get() = LightClassGenerationSupport.getInstance(project).findFilesForFacade(facadeFqName, searchScope)

                        override val packageFqName: FqName
                            get() = facadeFqName.parent()

                        override fun getContext(files: Collection<KtFile>): LightClassConstructionContext {
                            return LightClassGenerationSupport.getInstance(project).getContextForFacade(files)
                        }

                        override fun createLightClassData(
                                javaFileStub: PsiJavaFileStub,
                                bindingContext: BindingContext,
                                extraDiagnostics: Diagnostics): KotlinFacadeLightClassData {
                            return KotlinFacadeLightClassData(javaFileStub, extraDiagnostics)
                        }

                        override val generateClassFilter: GenerationState.GenerateClassFilter
                            get() = object : GenerationState.GenerateClassFilter() {
                                override fun shouldAnnotateClass(classOrObject: KtClassOrObject): Boolean {
                                    return shouldGenerateClass(classOrObject)
                                }

                                override fun shouldGenerateClass(classOrObject: KtClassOrObject): Boolean {
                                    return KtPsiUtil.isLocal(classOrObject)
                                }

                                override fun shouldGeneratePackagePart(jetFile: KtFile): Boolean {
                                    return true
                                }

                                override fun shouldGenerateScript(script: KtScript): Boolean {
                                    return false
                                }
                            }

                        override fun generate(state: GenerationState, files: Collection<KtFile>) {
                            if (!files.isEmpty()) {
                                val representativeFile = files.iterator().next()
                                val fileClassInfo = NoResolveFileClassesProvider.getFileClassInfo(representativeFile)
                                if (!fileClassInfo.withJvmMultifileClass) {
                                    val codegen = state.factory.forPackage(representativeFile.packageFqName, files)
                                    codegen.generate(CompilationErrorHandler.THROW_EXCEPTION)
                                    state.factory.asList()
                                    return
                                }
                            }

                            val codegen = state.factory.forMultifileClass(facadeFqName, files)
                            codegen.generate(CompilationErrorHandler.THROW_EXCEPTION)
                            state.factory.asList()
                        }

                        override fun toString(): String {
                            return StubGenerationStrategy::class.java.name + " for facade class"
                        }
                    })
        }

        fun createForDeclaredClass(classOrObject: KtClassOrObject): KotlinJavaFileStubProvider<OutermostKotlinClassLightClassData> {
            return KotlinJavaFileStubProvider(
                    classOrObject.project,
                    classOrObject.isLocal(),
                    object : StubGenerationStrategy<OutermostKotlinClassLightClassData> {
                        private val file: KtFile
                            get() = classOrObject.getContainingKtFile()

                        override fun getContext(files: Collection<KtFile>): LightClassConstructionContext {
                            return LightClassGenerationSupport.getInstance(classOrObject.project).getContextForClassOrObject(classOrObject)
                        }

                        override fun createLightClassData(
                                javaFileStub: PsiJavaFileStub,
                                bindingContext: BindingContext,
                                extraDiagnostics: Diagnostics): OutermostKotlinClassLightClassData {
                            val classDescriptor = bindingContext.get(BindingContext.CLASS, classOrObject) ?: return OutermostKotlinClassLightClassData(
                                    javaFileStub, extraDiagnostics, FqName.ROOT, classOrObject,
                                    emptyMap<KtClassOrObject, InnerKotlinClassLightClassData>())

                            val fqName = predictClassFqName(bindingContext, classDescriptor)
                            val allInnerClasses = CodegenBinding.getAllInnerClasses(bindingContext, classDescriptor)

                            val innerClassesMap = ContainerUtil.newHashMap<KtClassOrObject, InnerKotlinClassLightClassData>()
                            for (innerClassDescriptor in allInnerClasses) {
                                val declaration = descriptorToDeclaration(innerClassDescriptor)
                                if (declaration !is KtClassOrObject) continue

                                val innerLightClassData = InnerKotlinClassLightClassData(
                                        predictClassFqName(bindingContext, innerClassDescriptor),
                                        declaration)

                                innerClassesMap.put(declaration, innerLightClassData)
                            }

                            return OutermostKotlinClassLightClassData(
                                    javaFileStub,
                                    extraDiagnostics,
                                    fqName,
                                    classOrObject,
                                    innerClassesMap)
                        }

                        private fun predictClassFqName(bindingContext: BindingContext, classDescriptor: ClassDescriptor): FqName {
                            val asmType = CodegenBinding.getAsmType(bindingContext, classDescriptor)
                            //noinspection ConstantConditions
                            return JvmClassName.byInternalName(asmType.className.replace('.', '/')).fqNameForClassNameWithoutDollars
                        }

                        override val files: Collection<KtFile>
                            get() = listOf(file)

                        override val packageFqName: FqName
                            get() = file.packageFqName

                        override // Trivial: generate and analyze class we are interested in.
                                // Process all parent classes as they are context for current class
                                // Process child classes because they probably affect members (heuristic)
                                // Local classes should be process by CodegenAnnotatingVisitor to
                                // decide what class they should be placed in.
                                //
                                // Example:
                                // class A
                                // fun foo() {
                                //     trait Z: A {}
                                //     fun bar() {
                                //         class <caret>O2: Z {}
                                //     }
                                // }
                                // TODO: current method will process local classes in irrelevant declarations, it should be fixed.
                                // We generate all enclosing classes
                        val generateClassFilter: GenerationState.GenerateClassFilter
                            get() = object : GenerationState.GenerateClassFilter() {

                                override fun shouldGeneratePackagePart(jetFile: KtFile): Boolean {
                                    return true
                                }

                                override fun shouldAnnotateClass(classOrObject: KtClassOrObject): Boolean {
                                    return shouldGenerateClass(classOrObject)
                                }

                                override fun shouldGenerateClass(generatedClassOrObject: KtClassOrObject): Boolean {
                                    if (generatedClassOrObject === classOrObject) return true
                                    if (PsiTreeUtil.isAncestor(generatedClassOrObject, classOrObject, true) || PsiTreeUtil.isAncestor(classOrObject, generatedClassOrObject, true)) {
                                        return true
                                    }

                                    if (generatedClassOrObject.isLocal() && classOrObject.isLocal()) {
                                        val commonParent = PsiTreeUtil.findCommonParent(generatedClassOrObject, classOrObject)
                                        return commonParent != null && commonParent !is PsiFile
                                    }

                                    return false
                                }

                                override fun shouldGenerateScript(script: KtScript): Boolean {
                                    return PsiTreeUtil.isAncestor(script, classOrObject, false)
                                }
                            }

                        override fun generate(state: GenerationState, files: Collection<KtFile>) {
                            val packageCodegen = state.factory.forPackage(packageFqName, files)
                            val file = classOrObject.getContainingKtFile()
                            val packagePartType = state.fileClassesProvider.getFileClassType(file)
                            val context = state.rootContext.intoPackagePart(packageCodegen.packageFragment, packagePartType, file)
                            packageCodegen.generateClassOrObject(classOrObject, context)
                            state.factory.asList()
                        }

                        override fun toString(): String {
                            return StubGenerationStrategy::class.java.name + " for explicit class " + classOrObject.name
                        }
                    })
        }

        private val LOG = Logger.getInstance(KotlinJavaFileStubProvider::class.java)

        private fun createFakeClsFile(
                project: Project,
                packageFqName: FqName,
                files: Collection<KtFile>,
                fileStubProvider: Function0<PsiClassHolderFileStub<PsiFile>>): ClsFileImpl {
            val manager = PsiManager.getInstance(project)

            val virtualFile = getRepresentativeVirtualFile(files)
            val fakeFile = object : ClsFileImpl(ClassFileViewProvider(manager, virtualFile)) {
                override fun getStub(): PsiClassHolderFileStub<PsiFile> {
                    return fileStubProvider.invoke()
                }

                override fun getPackageName(): String {
                    return packageFqName.asString()
                }
            }

            fakeFile.isPhysical = false
            return fakeFile
        }

        private fun getRepresentativeVirtualFile(files: Collection<KtFile>): VirtualFile {
            val firstFile = files.iterator().next()
            val virtualFile = firstFile.virtualFile
            assert(virtualFile != null) { "No virtual file for " + firstFile }
            return virtualFile
        }

        private fun checkForBuiltIns(fqName: FqName, files: Collection<KtFile>) {
            for (file in files) {
                if (LightClassUtil.belongsToKotlinBuiltIns(file)) {
                    // We may not fail later due to some luck, but generating JetLightClasses for built-ins is a bad idea anyways
                    // If it fails later, there will be an exception logged
                    logErrorWithOSInfo(null, fqName, file.virtualFile)
                }
            }
        }

        private fun logErrorWithOSInfo(cause: Throwable?, fqName: FqName, virtualFile: VirtualFile?) {
            val path = if (virtualFile == null) "<null>" else virtualFile.path
            LOG.error(
                    "Could not generate LightClass for " + fqName + " declared in " + path + "\n" + "built-ins dir URL is " + LightClassUtil.builtInsDirUrl + "\n" + "System: " + SystemInfo.OS_NAME + " " + SystemInfo.OS_VERSION + " Java Runtime: " + SystemInfo.JAVA_RUNTIME_VERSION,
                    cause)
        }
    }
}

private interface StubGenerationStrategy<T : WithFileStubAndExtraDiagnostics> {
    val files: Collection<KtFile>
    val packageFqName: FqName

    fun getContext(files: Collection<KtFile>): LightClassConstructionContext
    fun createLightClassData(javaFileStub: PsiJavaFileStub, bindingContext: BindingContext, extraDiagnostics: Diagnostics): T

    val generateClassFilter: GenerationState.GenerateClassFilter
    fun generate(state: GenerationState, files: Collection<KtFile>)
}
