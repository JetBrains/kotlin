/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.AbstractPsiBasedContainingClassCalculatorConsistencyTest.Directives.ALLOW_PSI_PRESENCE
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLFirProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirScriptTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.LLContainingClassCalculator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhaseRecursively
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import kotlin.test.assertEquals
import kotlin.test.fail

abstract class AbstractPsiBasedContainingClassCalculatorConsistencyTest : AbstractAnalysisApiBasedTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + listOf(Directives)

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val resolutionFacade = mainModule.ktModule.getResolutionFacade(mainFile.project)
        val firFile = mainFile.getOrBuildFirFile(resolutionFacade)

        val allowedPsiPresence = mainModule.testModule.directives[ALLOW_PSI_PRESENCE].toSet()
        fun runChecker() {
            firFile.accept(object : FirVisitorVoid() {
                override fun visitElement(element: FirElement) {
                    if (element is FirDeclaration) {
                        checkDeclaration(element, allowedPsiPresence)
                    }

                    element.acceptChildren(this)
                }
            })
        }

        // First pass to iterate through raw declarations
        runChecker()

        // The file should be fully resolved to cover local declarations
        firFile.lazyResolveToPhaseRecursively(FirResolvePhase.BODY_RESOLVE)

        // Second pass to iterate through local declarations as well
        runChecker()
    }

    private fun checkDeclaration(fir: FirDeclaration, allowedPsiPresence: Set<String>) {
        val symbol = fir.symbol
        val session = fir.moduleData.session

        val compilerFirProvider = FirProviderImpl(session, session.kotlinScopeProvider)
        val compilerContainingSymbol = compilerFirProvider.getContainingClass(symbol)

        val llFirProvider = session.firProvider.also { check(it is LLFirProvider) }
        val llContainingSymbol = llFirProvider.getContainingClass(symbol)

        assertEquals(llContainingSymbol, LLContainingClassCalculator.getContainingClassSymbol(symbol))

        val signature = computeSignature(symbol)

        if (signature in allowedPsiPresence) {
            if (llContainingSymbol == null) {
                fail("Containing symbol for $signature is not calculated by PSI, the directive is useless")
            } else if (compilerContainingSymbol != null) {
                fail("Containing symbol for $signature is calculated by PSI and compiler, and the latter is unexpected")
            }
        } else {
            assertEquals(compilerContainingSymbol, llContainingSymbol, "Containing declarations for $signature do not match")
        }
    }

    private fun computeSignature(symbol: FirBasedSymbol<*>): String {
        return when (symbol) {
            is FirClassLikeSymbol<*> -> symbol.classId.asString()
            is FirCallableSymbol<*> -> symbol.callableId.toString()
            else -> "$symbol"
        }
    }

    object Directives : SimpleDirectivesContainer() {
        val ALLOW_PSI_PRESENCE by stringDirective("Do not fail the test if the containing class can be calculated only by PSI")
    }
}

abstract class AbstractSourcePsiBasedContainingClassCalculatorConsistencyTest : AbstractPsiBasedContainingClassCalculatorConsistencyTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)
}

abstract class AbstractScriptPsiBasedContainingClassCalculatorConsistencyTest : AbstractPsiBasedContainingClassCalculatorConsistencyTest() {
    override val configurator = AnalysisApiFirScriptTestConfigurator(analyseInDependentSession = false)
}