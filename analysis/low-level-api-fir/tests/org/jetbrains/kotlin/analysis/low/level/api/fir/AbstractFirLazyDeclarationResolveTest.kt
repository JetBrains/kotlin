/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirSingleResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirResolvableModuleSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirOutOfContentRootTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirScriptTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.NO_RUNTIME
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.SAME_THREAD)
abstract class AbstractFirLazyDeclarationResolveTest : AbstractFirLazyDeclarationResolveTestCase() {
    override fun checkSession(firSession: LLFirResolveSession) {
        require(firSession.isSourceSession)
    }

    override fun doTestByFileStructure(ktFile: KtFile, moduleStructure: TestModuleStructure, testServices: TestServices) {
        doLazyResolveTest(ktFile, testServices) { firResolveSession ->
            when {
                Directives.RESOLVE_FILE_ANNOTATIONS in moduleStructure.allDirectives -> {
                    val annotationContainer = firResolveSession.getOrBuildFirFile(ktFile).annotationsContainer!!
                    val session = annotationContainer.moduleData.session as LLFirResolvableModuleSession
                    annotationContainer to fun(phase: FirResolvePhase) {
                        session.moduleComponents.firModuleLazyDeclarationResolver.lazyResolve(
                            annotationContainer,
                            session.getScopeSession(),
                            phase,
                        )
                    }
                }
                Directives.RESOLVE_FILE in moduleStructure.allDirectives -> {
                    val session = firResolveSession.useSiteFirSession as LLFirResolvableModuleSession
                    val file = session.moduleComponents.firFileBuilder.buildRawFirFileWithCaching(ktFile)
                    file to fun(phase: FirResolvePhase) {
                        file.lazyResolveToPhase(phase)
                    }
                }
                else -> {
                    val ktDeclaration = if (Directives.RESOLVE_SCRIPT in moduleStructure.allDirectives) {
                        ktFile.script!!
                    } else {
                        testServices.expressionMarkerProvider.getElementOfTypeAtCaret<KtDeclaration>(ktFile)
                    }

                    val declarationSymbol = ktDeclaration.resolveToFirSymbol(firResolveSession)
                    val firDeclaration = chooseMemberDeclarationIfNeeded(declarationSymbol, moduleStructure, firResolveSession)
                    firDeclaration.fir to fun(phase: FirResolvePhase) {
                        firDeclaration.lazyResolveToPhase(phase)
                    }
                }
            }
        }
    }

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        with(builder) {
            forTestsNotMatching("analysis/low-level-api-fir/testData/lazyResolve/noRuntime/*" ) {
                defaultDirectives {
                    +WITH_STDLIB
                }
            }

            forTestsMatching("analysis/low-level-api-fir/testData/lazyResolve/noRuntime/*") {
                defaultDirectives {
                    +NO_RUNTIME
                }
            }

            useDirectives(Directives)
        }
    }

    private object Directives : SimpleDirectivesContainer() {
        val RESOLVE_FILE_ANNOTATIONS by directive("Resolve file annotations instead of declaration at caret")
        val RESOLVE_SCRIPT by directive("Resolve script instead of declaration at caret")
        val RESOLVE_FILE by directive("Resolve file instead of declaration at caret")
    }
}

abstract class AbstractFirSourceLazyDeclarationResolveTest : AbstractFirLazyDeclarationResolveTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)
}

abstract class AbstractFirOutOfContentRootLazyDeclarationResolveTest : AbstractFirLazyDeclarationResolveTest() {
    override val configurator get() = AnalysisApiFirOutOfContentRootTestConfigurator
}

abstract class AbstractFirScriptLazyDeclarationResolveTest : AbstractFirLazyDeclarationResolveTest() {
    override val configurator = AnalysisApiFirScriptTestConfigurator(analyseInDependentSession = false)
}
