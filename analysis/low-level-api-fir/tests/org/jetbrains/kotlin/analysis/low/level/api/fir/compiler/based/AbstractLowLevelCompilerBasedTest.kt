/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based


import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirResolveSessionService
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirTestSuppressor
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.facades.LLFirAnalyzerFacadeFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.base.FirLowLevelCompilerBasedTestConfigurator
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.test.framework.base.registerAnalysisApiBaseTestServices
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtModuleByCompilerConfiguration
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.ktTestModuleStructure
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.builders.testConfiguration
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.FIR_PARSER
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifactImpl
import org.jetbrains.kotlin.test.frontend.fir.FirOutputPartForDependsOnModule
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticCollectorService
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*

abstract class AbstractLowLevelCompilerBasedTest : AbstractCompilerBasedTest() {
    final override fun TestConfigurationBuilder.configuration() {
        globalDefaults {
            frontend = FrontendKinds.FIR
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            dependencyKind = DependencyKind.Source
        }

        defaultDirectives {
            FIR_PARSER with FirParser.Psi
        }

        FirLowLevelCompilerBasedTestConfigurator.configureTest(this, disposable)
        configureTest(this)
        defaultConfiguration(this)
        registerAnalysisApiBaseTestServices(disposable, FirLowLevelCompilerBasedTestConfigurator)
        useAdditionalServices(service<FirDiagnosticCollectorService>(::AnalysisApiFirDiagnosticCollectorService))

        firHandlersStep {
            useHandlers(::LLDiagnosticParameterChecker)
            useHandlers(::LLFirPhaseVerifier)
        }

        useMetaTestConfigurators(::LLFirMetaTestConfigurator)
        useAfterAnalysisCheckers(::LLFirIdenticalChecker)

        // For multiplatform tests it's expected that LL and FIR diverge,
        // because IR actualizer doesn't run in IDE mode tests.
        forTestsNotMatching("compiler/testData/diagnostics/tests/multiplatform/*") {
            useAfterAnalysisCheckers(::LLFirDivergenceCommentChecker)
        }

        useAfterAnalysisCheckers(::LLFirTestSuppressor)
    }

    abstract fun configureTest(builder: TestConfigurationBuilder)

    inner class LowLevelFirFrontendFacade(
        testServices: TestServices,
        private val facadeFactory: LLFirAnalyzerFacadeFactory,
    ) : FirFrontendFacade(testServices) {
        override val additionalServices: List<ServiceRegistrationData>
            get() = emptyList()

        override fun analyze(module: TestModule): FirOutputArtifact {
            val isMppSupported = module.languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects)

            val sortedModules = if (isMppSupported) sortDependsOnTopologically(module) else listOf(module)

            val firOutputPartForDependsOnModules = mutableListOf<FirOutputPartForDependsOnModule>()
            for (testModule in sortedModules) {
                firOutputPartForDependsOnModules.add(analyzeDependsOnModule(testModule))
            }

            return FirOutputArtifactImpl(firOutputPartForDependsOnModules)
        }

        private fun analyzeDependsOnModule(module: TestModule): FirOutputPartForDependsOnModule {
            val ktTestModule = testServices.ktTestModuleStructure.getKtTestModule(module.name)
            val ktModule = ktTestModule.ktModule as KtModuleByCompilerConfiguration

            val project = ktModule.project
            val firResolveSession = LLFirResolveSessionService.getInstance(project).getFirResolveSession(ktModule as KaModule)

            val allFirFiles = module.files.filter { it.isKtFile }.zip(
                ktModule.psiFiles
                    .filterIsInstance<KtFile>()
                    .map { psiFile -> psiFile.getOrBuildFirFile(firResolveSession) }
            )

            val diagnosticCheckerFilter = DiagnosticCheckerFilter(
                runDefaultCheckers = true,
                runExtraCheckers = FirDiagnosticsDirectives.WITH_EXTRA_CHECKERS in module.directives,
                runExperimentalCheckers = FirDiagnosticsDirectives.WITH_EXPERIMENTAL_CHECKERS in module.directives,
            )

            val analyzerFacade = facadeFactory.createFirFacade(firResolveSession, allFirFiles.toMap(), diagnosticCheckerFilter)
            return FirOutputPartForDependsOnModule(
                module,
                firResolveSession.useSiteFirSession,
                analyzerFacade,
                analyzerFacade.allFirFiles
            )
        }
    }

    override fun runTest(filePath: String) {
        val configuration = testConfiguration(filePath, configuration)

        if (ignoreTest(filePath, configuration)) {
            return
        }

        super.runTest(filePath)
    }
}
