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

package org.jetbrains.kotlin.asJava.builder

import com.intellij.openapi.components.ServiceManager
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
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.Stack
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.codegen.CompilationErrorHandler
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fileClasses.NoResolveFileClassesProvider
import org.jetbrains.kotlin.fileClasses.getFileClassType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils.descriptorToDeclaration
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics

abstract class LightClassDataProvider<T : WithFileStubAndExtraDiagnostics>(
        private val project: Project
) : CachedValueProvider<T> {

    abstract val files: Collection<KtFile>
    abstract val packageFqName: FqName

    abstract fun getContext(files: Collection<KtFile>): LightClassConstructionContext
    abstract fun createLightClassData(javaFileStub: PsiJavaFileStub, bindingContext: BindingContext, extraDiagnostics: Diagnostics): T

    abstract val generateClassFilter: GenerationState.GenerateClassFilter
    abstract fun generate(state: GenerationState, files: Collection<KtFile>)
    abstract val isLocal: Boolean

    override fun compute(): CachedValueProvider.Result<T>? {
        if (files.isEmpty()) return null
        return CachedValueProvider.Result.create(
                computeLightClassData(),
                if (isLocal) PsiModificationTracker.MODIFICATION_COUNT else PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT
        )
    }

    private fun computeLightClassData(): T {
        val packageFqName = packageFqName
        val files = files

        val context = getContext(files)

        val javaFileStub = createJavaFileStub(packageFqName, files)
        val bindingContext: BindingContext

        val state: GenerationState

        try {
            val stubStack = Stack<StubElement<PsiElement>>()

            @Suppress("UNCHECKED_CAST")
            stubStack.push(javaFileStub as StubElement<PsiElement>)

            state = GenerationState(
                    project,
                    KotlinLightClassBuilderFactory(stubStack),
                    context.module,
                    context.bindingContext,
                    files.toMutableList(),
                    CompilerConfiguration.EMPTY,
                    generateClassFilter,
                    wantsDiagnostics = false
            )
            state.beforeCompile()

            bindingContext = state.bindingContext

            generate(state, files)

            val pop = stubStack.pop()
            if (pop !== javaFileStub) {
                LOG.error("Unbalanced stack operations: " + pop)
            }

            ServiceManager.getService(project, StubComputationTracker::class.java)?.onStubComputed(javaFileStub)
            return createLightClassData(javaFileStub, bindingContext, state.collectedExtraJvmDiagnostics)
        }
        catch (e: ProcessCanceledException) {
            throw e
        }
        catch (e: RuntimeException) {
            logErrorWithOSInfo(e, packageFqName, null)
            throw e
        }
    }

    private fun createJavaFileStub(packageFqName: FqName, files: Collection<KtFile>): PsiJavaFileStub {
        val javaFileStub = PsiJavaFileStubImpl(packageFqName.asString(), true)
        javaFileStub.psiFactory = ClsWrapperStubPsiFactory.INSTANCE

        val manager = PsiManager.getInstance(project)

        val virtualFile = getRepresentativeVirtualFile(files)
        val fakeFile = object : ClsFileImpl(ClassFileViewProvider(manager, virtualFile)) {
            override fun getStub() = javaFileStub

            override fun getPackageName() = packageFqName.asString()

            override fun isPhysical() = false
        }

        javaFileStub.psi = fakeFile
        return javaFileStub
    }

    private fun getRepresentativeVirtualFile(files: Collection<KtFile>): VirtualFile {
        return files.first().viewProvider.virtualFile
    }

    private fun logErrorWithOSInfo(cause: Throwable?, fqName: FqName, virtualFile: VirtualFile?) {
        val path = if (virtualFile == null) "<null>" else virtualFile.path
        LOG.error(
                "Could not generate LightClass for $fqName declared in $path\n" +
                "System: ${SystemInfo.OS_NAME} ${SystemInfo.OS_VERSION} Java Runtime: ${SystemInfo.JAVA_RUNTIME_VERSION}",
                cause
        )
    }

    companion object {
        private val LOG = Logger.getInstance(LightClassDataProvider::class.java)
    }
}

class LightClassDataProviderForClassOrObject(private val classOrObject: KtClassOrObject) :
        LightClassDataProvider<WithFileStubAndExtraDiagnostics>(classOrObject.project) {

    private val file: KtFile
        get() = classOrObject.getContainingKtFile()

    override val isLocal: Boolean get() = classOrObject.isLocal()

    override fun getContext(files: Collection<KtFile>): LightClassConstructionContext {
        return LightClassGenerationSupport.getInstance(classOrObject.project).getContextForClassOrObject(classOrObject)
    }

    override fun createLightClassData(
            javaFileStub: PsiJavaFileStub,
            bindingContext: BindingContext,
            extraDiagnostics: Diagnostics): WithFileStubAndExtraDiagnostics {
        val classDescriptor = bindingContext.get(BindingContext.CLASS, classOrObject) ?: return InvalidLightClassData

        val allInnerClasses = CodegenBinding.getAllInnerClasses(bindingContext, classDescriptor)

        val innerClassesMap = ContainerUtil.newHashMap<KtClassOrObject, InnerKotlinClassLightClassData>()
        for (innerClassDescriptor in allInnerClasses) {
            val declaration = descriptorToDeclaration(innerClassDescriptor) as? KtClassOrObject ?: continue
            innerClassesMap.put(declaration, InnerKotlinClassLightClassData(declaration))
        }

        return OutermostKotlinClassLightClassData(
                javaFileStub,
                extraDiagnostics,
                classOrObject,
                innerClassesMap)
    }

    override val files: Collection<KtFile>
        get() = listOf(file)

    override val packageFqName: FqName
        get() = file.packageFqName

    override val generateClassFilter: GenerationState.GenerateClassFilter
        get() = object : GenerationState.GenerateClassFilter() {

            override fun shouldGeneratePackagePart(jetFile: KtFile): Boolean {
                return true
            }

            override fun shouldAnnotateClass(processingClassOrObject: KtClassOrObject): Boolean {
                return shouldGenerateClass(processingClassOrObject)
            }

            override fun shouldGenerateClass(processingClassOrObject: KtClassOrObject): Boolean {
                // Trivial: generate and analyze class we are interested in.
                if (classOrObject === processingClassOrObject) return true

                // Process all parent classes as they are context for current class
                // Process child classes because they probably affect members (heuristic)

                if (PsiTreeUtil.isAncestor(classOrObject, processingClassOrObject, true) ||
                    PsiTreeUtil.isAncestor(processingClassOrObject, classOrObject, true)) {
                    return true
                }

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

                if (classOrObject.isLocal() && processingClassOrObject.isLocal()) {
                    val commonParent = PsiTreeUtil.findCommonParent(classOrObject, processingClassOrObject)
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
        return this.javaClass.name + " for " + classOrObject.name
    }
}

sealed class LightClassDataProviderForFileFacade private constructor(
        protected val project: Project, protected val facadeFqName: FqName
) : LightClassDataProvider<KotlinFacadeLightClassData>(project) {
    override val packageFqName: FqName
        get() = facadeFqName.parent()

    override val isLocal: Boolean get() = false

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
            override fun shouldAnnotateClass(processingClassOrObject: KtClassOrObject): Boolean {
                return shouldGenerateClass(processingClassOrObject)
            }

            override fun shouldGenerateClass(processingClassOrObject: KtClassOrObject): Boolean {
                return KtPsiUtil.isLocal(processingClassOrObject)
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
        return this.javaClass.name + " for $facadeFqName"
    }

    // create delegate by relevant files in project source using LightClassGenerationSupport
    class ByProjectSource(
            project: Project,
            facadeFqName: FqName,
            private val searchScope: GlobalSearchScope
    ) : LightClassDataProviderForFileFacade(project, facadeFqName) {
        override val files: Collection<KtFile>
            get() = LightClassGenerationSupport.getInstance(project).findFilesForFacade(facadeFqName, searchScope)
    }

    // create delegate by single file
    class ByFile(
            project: Project,
            facadeFqName: FqName,
            private val file: KtFile
    ) : LightClassDataProviderForFileFacade(project, facadeFqName) {
        override val files: Collection<KtFile>
            get() = listOf(file)
    }
}


interface StubComputationTracker {
    fun onStubComputed(javaFileStub: PsiJavaFileStub)
}
