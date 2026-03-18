/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import com.intellij.psi.util.descendantsOfType
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirOfType
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.LLPartialBodyElementMapper
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirResolvableSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.resolvePhase
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.utils.findIsInstanceAnd
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class LLGenericAnalysisTest : AbstractAnalysisApiExecutionTest("testData/genericAnalysis") {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)

    @Test
    fun simple(ktFile: KtFile) {
        withResolutionFacade(ktFile) { resolutionFacade ->
            val ktSecondCall = ktFile.descendantsOfType<KtCallExpression>().first { it.text == "consume(2)" }
            ktSecondCall.getOrBuildFir(resolutionFacade)

            val firFile = ktFile.getOrBuildFirFile(resolutionFacade)
            val firFunction = firFile.declarations.findIsInstanceAnd<FirFunction> { it.name() == "test" }!!

            // The function is only partially resolved
            assert(firFunction.resolvePhase == FirResolvePhase.ANNOTATION_ARGUMENTS)
        }
    }

    @Test
    fun bodyAlreadyAnalyzed(ktFile: KtFile) {
        withResolutionFacade(ktFile) { resolutionFacade ->
            val firFile = ktFile.getOrBuildFirFile(resolutionFacade)
            val firFunction = firFile.declarations.findIsInstanceAnd<FirFunction> { it.name() == "test" }!!
            assert(firFunction.resolvePhase == FirResolvePhase.RAW_FIR)

            // Do not trigger 'getOrBuildFir()'
            firFunction.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
            assert(firFunction.resolvePhase == FirResolvePhase.BODY_RESOLVE)

            val ktFunction = ktFile.declarations.findIsInstanceAnd<KtFunction> { it.name == "test" }!!
            val ktBlock = ktFunction.bodyBlockExpression!!
            val ktStatements = ktBlock.statements

            // Simulate data race between BODY_RESOLVE and LLElementMapper computation
            val mapper = LLPartialBodyElementMapper(firFunction, ktFunction, ktBlock, ktStatements, firFunction.llFirResolvableSession!!)
            assertEquals(firFunction, mapper.invoke(ktFunction))

            val ktSecondCall = ktFile.descendantsOfType<KtCallExpression>().first { it.text == "consume(2)" }
            val firSecondCall = ktSecondCall.getOrBuildFirOfType<FirFunctionCall>(resolutionFacade)
            assertEquals(ktSecondCall, firSecondCall.psi)
        }
    }
}