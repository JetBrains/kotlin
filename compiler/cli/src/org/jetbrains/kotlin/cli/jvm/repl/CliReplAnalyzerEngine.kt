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

package org.jetbrains.kotlin.cli.jvm.repl

import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.getModuleName
import org.jetbrains.kotlin.cli.jvm.repl.di.createContainerForReplWithJava
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfoFactory
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.resolve.lazy.FileScopeFactory
import org.jetbrains.kotlin.resolve.lazy.FileScopeProvider
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.data.KtClassLikeInfo
import org.jetbrains.kotlin.resolve.lazy.declarations.*
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyScriptDescriptor
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.script.ScriptPriorities

class CliReplAnalyzerEngine(private val environment: KotlinCoreEnvironment) {
    private val topDownAnalysisContext: TopDownAnalysisContext
    private val topDownAnalyzer: LazyTopDownAnalyzerForTopLevel
    private val resolveSession: ResolveSession
    private val scriptDeclarationFactory: ScriptMutableDeclarationProviderFactory
    val module: ModuleDescriptorImpl
    val trace: BindingTraceContext = CliLightClassGenerationSupport.NoScopeRecordCliBindingTrace()
    private val replState = ReplState()

    init {
        val moduleContext = TopDownAnalyzerFacadeForJVM.createContextWithSealedModule(environment.project, environment.getModuleName())
        this.module = moduleContext.module

        scriptDeclarationFactory = ScriptMutableDeclarationProviderFactory()

        val container = createContainerForReplWithJava(
                moduleContext,
                trace,
                scriptDeclarationFactory,
                ProjectScope.getAllScope(environment.project),
                replState,
                JvmPackagePartProvider(environment)
        )

        this.topDownAnalysisContext = TopDownAnalysisContext(
                TopDownAnalysisMode.LocalDeclarations, DataFlowInfoFactory.EMPTY, container.resolveSession.declarationScopeProvider
        )
        this.topDownAnalyzer = container.lazyTopDownAnalyzerForTopLevel
        this.resolveSession = container.resolveSession

        moduleContext.initializeModuleContents(CompositePackageFragmentProvider(
                listOf(
                        container.resolveSession.packageFragmentProvider,
                        container.javaDescriptorResolver.packageFragmentProvider
                )
        ))

    }

    interface ReplLineAnalysisResult {
        val scriptDescriptor: ScriptDescriptor?
        val diagnostics: Diagnostics

        data class Successful(override val scriptDescriptor: ScriptDescriptor, override val diagnostics: Diagnostics) : ReplLineAnalysisResult

        data class WithErrors(override val diagnostics: Diagnostics) : ReplLineAnalysisResult {
            override val scriptDescriptor: ScriptDescriptor? get() = null
        }
    }

    fun analyzeReplLine(psiFile: KtFile, priority: Int): ReplLineAnalysisResult {
        topDownAnalysisContext.scripts.clear()
        trace.clearDiagnostics()

        psiFile.script!!.putUserData(ScriptPriorities.PRIORITY_KEY, priority)

        return doAnalyze(psiFile)
    }

    private fun doAnalyze(linePsi: KtFile): ReplLineAnalysisResult {
        replState.submitLine(linePsi)
        scriptDeclarationFactory.setDelegateFactory(FileBasedDeclarationProviderFactory(resolveSession.storageManager, listOf(linePsi)))

        val context = topDownAnalyzer.analyzeDeclarations(topDownAnalysisContext.topDownAnalysisMode, listOf(linePsi))

        if (trace.get(BindingContext.FILE_TO_PACKAGE_FRAGMENT, linePsi) == null) {
            trace.record(BindingContext.FILE_TO_PACKAGE_FRAGMENT, linePsi, resolveSession.getPackageFragment(FqName.ROOT))
        }

        val diagnostics = trace.bindingContext.diagnostics
        val hasErrors = diagnostics.any { it.severity == Severity.ERROR }
        if (hasErrors) {
            replState.lineFailure(linePsi)
            return ReplLineAnalysisResult.WithErrors(diagnostics)
        }
        else {
            val scriptDescriptor = context.scripts[linePsi.script]!!
            replState.lineSuccess(linePsi, scriptDescriptor)
            return ReplLineAnalysisResult.Successful(scriptDescriptor, diagnostics)
        }

    }

    private class ScriptMutableDeclarationProviderFactory : DeclarationProviderFactory {
        private lateinit var delegateFactory: DeclarationProviderFactory
        private lateinit var rootPackageProvider: AdaptablePackageMemberDeclarationProvider

        fun setDelegateFactory(delegateFactory: DeclarationProviderFactory) {
            this.delegateFactory = delegateFactory

            val provider = delegateFactory.getPackageMemberDeclarationProvider(FqName.ROOT)!!
            try {
                rootPackageProvider.addDelegateProvider(provider)
            }
            catch (e: UninitializedPropertyAccessException) {
                rootPackageProvider = AdaptablePackageMemberDeclarationProvider(provider)
            }
        }

        override fun getClassMemberDeclarationProvider(classLikeInfo: KtClassLikeInfo): ClassMemberDeclarationProvider {
            return delegateFactory.getClassMemberDeclarationProvider(classLikeInfo)
        }

        override fun getPackageMemberDeclarationProvider(packageFqName: FqName): PackageMemberDeclarationProvider? {
            if (packageFqName.isRoot) {
                return rootPackageProvider
            }

            return delegateFactory.getPackageMemberDeclarationProvider(packageFqName)
        }

        override fun diagnoseMissingPackageFragment(file: KtFile) {
            delegateFactory.diagnoseMissingPackageFragment(file)
        }

        class AdaptablePackageMemberDeclarationProvider(
                private var delegateProvider: PackageMemberDeclarationProvider
        ) : DelegatePackageMemberDeclarationProvider(delegateProvider) {
            fun addDelegateProvider(provider: PackageMemberDeclarationProvider) {
                delegateProvider = CombinedPackageMemberDeclarationProvider(listOf(provider, delegateProvider))

                delegate = delegateProvider
            }
        }
    }
}

class ReplState {
    private val lines = hashMapOf<KtFile, LineInfo>()
    private val successfulLines = arrayListOf<SuccessfulLine>()

    fun submitLine(ktFile: KtFile) {
        lines[ktFile] = SubmittedLine(ktFile, successfulLines.lastOrNull())
    }

    fun lineSuccess(ktFile: KtFile, scriptDescriptor: LazyScriptDescriptor) {
        val successfulLine = SuccessfulLine(ktFile, successfulLines.lastOrNull(), scriptDescriptor)
        lines[ktFile] = successfulLine
        successfulLines.add(successfulLine)
    }

    fun lineFailure(ktFile: KtFile) {
        lines[ktFile] = FailedLine(ktFile, successfulLines.lastOrNull())
    }

    fun lineInfo(ktFile: KtFile) = lines[ktFile]

    interface LineInfo {
        val linePsi: KtFile
        val parentLine: SuccessfulLine?

        val lexicalScopeBeforeThisLine: LexicalScope? get() = parentLine?.lineDescriptor?.scopeForInitializerResolution
    }

    data class SubmittedLine(override val linePsi: KtFile, override val parentLine: SuccessfulLine?): LineInfo
    data class SuccessfulLine(override val linePsi: KtFile, override val parentLine: SuccessfulLine?, val lineDescriptor: LazyScriptDescriptor): LineInfo
    data class FailedLine(override val linePsi: KtFile, override val parentLine: SuccessfulLine?): LineInfo
}

class ReplFileScopeProvider(
        private val replState: ReplState,
        private val fileScopeFactory: FileScopeFactory
) : FileScopeProvider {
    override fun getFileResolutionScope(file: KtFile)
            = replState.lineInfo(file)?.lexicalScopeBeforeThisLine ?: fileScopeFactory.getLexicalScopeAndImportResolver(file).scope

    override fun getImportResolver(file: KtFile) = fileScopeFactory.getLexicalScopeAndImportResolver(file).importResolver
}