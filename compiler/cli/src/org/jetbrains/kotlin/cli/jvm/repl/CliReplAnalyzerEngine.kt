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
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportsFactory
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfoFactory
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.resolve.lazy.FileScopeProviderImpl
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.TopLevelDescriptorProvider
import org.jetbrains.kotlin.resolve.lazy.data.KtClassLikeInfo
import org.jetbrains.kotlin.resolve.lazy.declarations.*
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.script.ScriptPriorities
import org.jetbrains.kotlin.storage.StorageManager

class CliReplAnalyzerEngine(private val environment: KotlinCoreEnvironment) {
    private val topDownAnalysisContext: TopDownAnalysisContext
    private val topDownAnalyzer: LazyTopDownAnalyzerForTopLevel
    private val resolveSession: ResolveSession
    private val scriptDeclarationFactory: ScriptMutableDeclarationProviderFactory
    val module: ModuleDescriptorImpl
    private var lastLineScope: LexicalScope? = null
    val trace: BindingTraceContext = CliLightClassGenerationSupport.NoScopeRecordCliBindingTrace()

    init {
        val moduleContext = TopDownAnalyzerFacadeForJVM.createContextWithSealedModule(environment.project, environment.getModuleName())
        this.module = moduleContext.module

        scriptDeclarationFactory = ScriptMutableDeclarationProviderFactory()

        val container = createContainerForReplWithJava(
                moduleContext,
                trace,
                scriptDeclarationFactory,
                ProjectScope.getAllScope(environment.project),
                object : ReplLastLineScopeProvider {
                    override val lastLineScope: LexicalScope?
                        get() = this@CliReplAnalyzerEngine.lastLineScope
                },
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

    private fun doAnalyze(psiFile: KtFile): ReplLineAnalysisResult {
        scriptDeclarationFactory.setDelegateFactory(FileBasedDeclarationProviderFactory(resolveSession.storageManager, listOf(psiFile)))

        val context = topDownAnalyzer.analyzeDeclarations(topDownAnalysisContext.topDownAnalysisMode, listOf(psiFile))

        if (trace.get(BindingContext.FILE_TO_PACKAGE_FRAGMENT, psiFile) == null) {
            trace.record(BindingContext.FILE_TO_PACKAGE_FRAGMENT, psiFile, resolveSession.getPackageFragment(FqName.ROOT))
        }

        val diagnostics = trace.bindingContext.diagnostics
        val hasErrors = diagnostics.any { it.severity == Severity.ERROR }
        if (hasErrors) {
            return ReplLineAnalysisResult.WithErrors(diagnostics)
        }
        else {
            val scriptDescriptor = context.scripts[psiFile.script]!!.apply {
                lastLineScope = this.scopeForInitializerResolution
            }
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

interface ReplLastLineScopeProvider {
    val lastLineScope: LexicalScope?
}

class ReplFileScopeProvider(
        private val lastLineScopeProvider: ReplLastLineScopeProvider,
        topLevelDescriptorProvider: TopLevelDescriptorProvider,
        storageManager: StorageManager,
        moduleDescriptor: ModuleDescriptor,
        qualifiedExpressionResolver: QualifiedExpressionResolver,
        bindingTrace: BindingTrace,
        ktImportsFactory: KtImportsFactory
) : FileScopeProviderImpl(topLevelDescriptorProvider, storageManager, moduleDescriptor, qualifiedExpressionResolver, bindingTrace, ktImportsFactory) {

    override fun getFileResolutionScope(file: KtFile): LexicalScope
            = lastLineScopeProvider.lastLineScope ?: super.getFileResolutionScope(file)
}