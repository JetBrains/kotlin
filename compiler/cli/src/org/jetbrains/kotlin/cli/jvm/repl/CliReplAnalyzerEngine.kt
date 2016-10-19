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

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfoFactory
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.data.KtClassLikeInfo
import org.jetbrains.kotlin.resolve.lazy.declarations.*
import org.jetbrains.kotlin.resolve.repl.ReplState
import org.jetbrains.kotlin.script.ScriptPriorities

class CliReplAnalyzerEngine(environment: KotlinCoreEnvironment) {
    private val topDownAnalysisContext: TopDownAnalysisContext
    private val topDownAnalyzer: LazyTopDownAnalyzer
    private val resolveSession: ResolveSession
    private val scriptDeclarationFactory: ScriptMutableDeclarationProviderFactory
    val module: ModuleDescriptorImpl
    private val replState = ReplState()
    val trace: BindingTraceContext = CliLightClassGenerationSupport.NoScopeRecordCliBindingTrace()

    init {
        // Module source scope is empty because all binary classes are in the dependency module, and all source classes are guaranteed
        // to be found via ResolveSession. The latter is true as long as light classes are not needed in REPL (which is currently true
        // because no symbol declared in the REPL session can be used from Java)
        val container = TopDownAnalyzerFacadeForJVM.createContainer(
                environment.project,
                emptyList(),
                trace,
                environment.configuration,
                { scope -> JvmPackagePartProvider(environment, scope) },
                { storageManager, files -> ScriptMutableDeclarationProviderFactory() }
        )

        this.module = container.get<ModuleDescriptorImpl>()
        this.scriptDeclarationFactory = container.get<ScriptMutableDeclarationProviderFactory>()
        this.resolveSession = container.get<ResolveSession>()
        this.topDownAnalysisContext = TopDownAnalysisContext(
                TopDownAnalysisMode.LocalDeclarations, DataFlowInfoFactory.EMPTY, resolveSession.declarationScopeProvider
        )
        this.topDownAnalyzer = container.get<LazyTopDownAnalyzer>()
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
        scriptDeclarationFactory.setDelegateFactory(FileBasedDeclarationProviderFactory(resolveSession.storageManager, listOf(linePsi)))
        replState.submitLine(linePsi)

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
