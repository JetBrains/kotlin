/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based


import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.createResolveStateForNoCaching
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.base.FirLowLevelAnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.LLFirLazyTransformer
import org.jetbrains.kotlin.analysis.test.framework.AbstractCompilerBasedTest
import org.jetbrains.kotlin.analysis.test.framework.base.registerAnalysisApiBaseTestServices
import org.jetbrains.kotlin.analysis.test.framework.project.structure.TestKtSourceModule
import org.jetbrains.kotlin.analysis.test.framework.project.structure.ktModuleProvider
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.ExecutionListenerBasedDisposableProvider
import org.jetbrains.kotlin.test.bind
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.builders.testConfiguration
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendFacade
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.isKtFile
import org.jetbrains.kotlin.test.services.*

abstract class AbstractCompilerBasedTestForFir : AbstractCompilerBasedTest() {
    @OptIn(TestInfrastructureInternals::class)
    final override fun TestConfigurationBuilder.configuration() {
        globalDefaults {
            frontend = FrontendKinds.FIR
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            dependencyKind = DependencyKind.Source
        }

        configureTest()
        defaultConfiguration(this)
        registerAnalysisApiBaseTestServices(disposable, FirLowLevelAnalysisApiTestConfigurator)

        useAdditionalService<ApplicationDisposableProvider> { ExecutionListenerBasedDisposableProvider() }
        useAdditionalService<KotlinStandardLibrariesPathProvider> { StandardLibrariesPathProviderForKotlinProject }
        useAdditionalService(::TestKtModuleProvider)
        usePreAnalysisHandlers(::ModuleRegistrarPreAnalysisHandler.bind(disposable, true))
        usePreAnalysisHandlers(::SealedClassesInheritorsCaclulatorPreAnalysisHandler)


        firHandlersStep {
            useHandlers(::LLDiagnosticParameterChecker)
        }
    }

    open fun TestConfigurationBuilder.configureTest() {}

    inner class LowLevelFirFrontendFacade(
        testServices: TestServices
    ) : FrontendFacade<FirOutputArtifact>(testServices, FrontendKinds.FIR) {

        override val directiveContainers: List<DirectivesContainer>
            get() = listOf(FirDiagnosticsDirectives)

        override fun analyze(module: TestModule): FirOutputArtifact {
            val moduleInfoProvider = testServices.ktModuleProvider
            val moduleInfo = moduleInfoProvider.getModule(module.name) as TestKtSourceModule

            val project = testServices.compilerConfigurationProvider.getProject(module)
            val resolveState = createResolveStateForNoCaching(moduleInfo, project)

            val allFirFiles =
                module.files.filter { it.isKtFile }.zip(
                    moduleInfo.psiFiles
                        .filterIsInstance<KtFile>()
                        .map { psiFile -> psiFile.getOrBuildFirFile(resolveState) }
                )

            val diagnosticCheckerFilter = if (FirDiagnosticsDirectives.WITH_EXTENDED_CHECKERS in module.directives) {
                DiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS
            } else DiagnosticCheckerFilter.ONLY_COMMON_CHECKERS

            val analyzerFacade = LowLevelFirAnalyzerFacade(resolveState, allFirFiles.toMap(), diagnosticCheckerFilter)
            return LowLevelFirOutputArtifact(resolveState.rootModuleSession, analyzerFacade)
        }
    }

    override fun runTest(filePath: String) {
        val configuration = testConfiguration(filePath, configuration)
        if (ignoreTest(filePath, configuration)) {
            return
        }
        val oldEnableDeepEnsure = LLFirLazyTransformer.enableDeepEnsure
        try {
            LLFirLazyTransformer.enableDeepEnsure = true
            super.runTest(filePath)
        } finally {
            LLFirLazyTransformer.enableDeepEnsure = oldEnableDeepEnsure
        }
    }
}
