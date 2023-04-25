/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based


import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirResolveSessionService
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.facades.LLFirAnalyzerFacadeFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.base.FirLowLevelCompilerBasedTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.AbstractCompilerBasedTest
import org.jetbrains.kotlin.analysis.test.framework.base.registerAnalysisApiBaseTestServices
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtSourceModuleByCompilerConfiguration
import org.jetbrains.kotlin.analysis.test.framework.project.structure.ktModuleProvider
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
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*

abstract class AbstractCompilerBasedTestForFir : AbstractCompilerBasedTest() {
    final override fun TestConfigurationBuilder.configuration() {
        globalDefaults {
            frontend = FrontendKinds.FIR
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            dependencyKind = DependencyKind.Source
        }

        defaultDirectives {
            FIR_PARSER with FirParser.Psi
        }

        configureTest()
        defaultConfiguration(this)
        registerAnalysisApiBaseTestServices(disposable, FirLowLevelCompilerBasedTestConfigurator)
        useDirectives(SealedClassesInheritorsCaclulatorPreAnalysisHandler.Directives)
        usePreAnalysisHandlers(::SealedClassesInheritorsCaclulatorPreAnalysisHandler)

        firHandlersStep {
            useHandlers(::LLDiagnosticParameterChecker)
        }

        useMetaTestConfigurators(::LLFirMetaTestConfigurator)
        useAfterAnalysisCheckers(::LLFirIdenticalChecker)
        useAfterAnalysisCheckers(::LLFirDivergenceCommentChecker)
    }

    open fun TestConfigurationBuilder.configureTest() {}

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
            val moduleInfoProvider = testServices.ktModuleProvider
            val ktModule = moduleInfoProvider.getModule(module.name) as KtSourceModuleByCompilerConfiguration

            val project = ktModule.project
            val firResolveSession = LLFirResolveSessionService.getInstance(project).getFirResolveSessionNoCaching(ktModule)

            val allFirFiles =
                module.files.filter { it.isKtFile }.zip(
                    ktModule.psiFiles
                        .filterIsInstance<KtFile>()
                        .map { psiFile -> psiFile.getOrBuildFirFile(firResolveSession) }
                )

            val diagnosticCheckerFilter = if (FirDiagnosticsDirectives.WITH_EXTENDED_CHECKERS in module.directives) {
                DiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS
            } else DiagnosticCheckerFilter.ONLY_COMMON_CHECKERS

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