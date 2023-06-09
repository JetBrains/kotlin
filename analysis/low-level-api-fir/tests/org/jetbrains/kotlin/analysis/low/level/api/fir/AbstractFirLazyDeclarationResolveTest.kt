/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirResolvableModuleSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirOutOfContentRootTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
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
            if (Directives.RESOLVE_FILE_ANNOTATIONS in moduleStructure.allDirectives) {
                val annotationContainer = firResolveSession.getOrBuildFirFile(ktFile).annotationsContainer
                val session = annotationContainer.moduleData.session as LLFirResolvableModuleSession
                fun(phase: FirResolvePhase) {
                    session.moduleComponents.firModuleLazyDeclarationResolver.lazyResolve(
                        annotationContainer,
                        session.getScopeSession(),
                        phase,
                    )
                }
            } else {
                val ktDeclaration = testServices.expressionMarkerProvider.getElementOfTypeAtCaret<KtDeclaration>(ktFile)
                val declarationSymbol = ktDeclaration.resolveToFirSymbol(firResolveSession)
                val firDeclaration = chooseMemberDeclarationIfNeeded(declarationSymbol, moduleStructure)
                fun(phase: FirResolvePhase) {
                    firDeclaration.lazyResolveToPhase(phase)
                }
            }
        }
    }

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        with(builder) {
            defaultDirectives {
                +ConfigurationDirectives.WITH_STDLIB
            }

            useDirectives(Directives)
        }
    }

    private object Directives : SimpleDirectivesContainer() {
        val RESOLVE_FILE_ANNOTATIONS by directive("Resolve file annotations instead of declaration at caret")
    }
}

abstract class AbstractFirSourceLazyDeclarationResolveTest : AbstractFirLazyDeclarationResolveTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)
}

abstract class AbstractFirOutOfContentRootLazyDeclarationResolveTest : AbstractFirLazyDeclarationResolveTest() {
    override val configurator = AnalysisApiFirOutOfContentRootTestConfigurator
}