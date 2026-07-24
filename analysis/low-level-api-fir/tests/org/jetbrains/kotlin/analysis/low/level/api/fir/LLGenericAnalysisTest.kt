/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import com.intellij.psi.util.descendantsOfType
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirOfType
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbolOfType
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.LLPartialBodyElementMapper
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.bodyBlock
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLFirIdeRegisteredPluginAnnotations
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirResolvableSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.LLSourceLikeTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.resolvePhase
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.extensions.registeredPluginAnnotations
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.utils.findIsInstanceAnd
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class LLGenericAnalysisTest : AbstractAnalysisApiExecutionTest("testData/genericAnalysis") {
    override val configurator = LLSourceLikeTestConfigurator()

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

    @Test
    fun abstractFunctionWithDefault(ktFile: KtFile, testServices: TestServices) {
        withResolutionFacade(ktFile) { resolutionFacade ->
            val targetPsiClass = ktFile.declarations.last() as KtClass
            val targetPsiFunction = targetPsiClass.declarations.last() as KtNamedFunction
            val targetFirFunction = targetPsiFunction.resolveToFirSymbolOfType<FirNamedFunctionSymbol>(resolutionFacade)
            val psiStatements = targetPsiFunction.bodyBlock?.statements.orEmpty()
            psiStatements.first().getOrBuildFirOfType<FirStatement>(resolutionFacade)

            fun renderTarget(element: FirElement) = lazyResolveRenderer(StringBuilder()).renderElementAsString(element).trim()

            testServices.assertions.assertEquals(
                """
                    public final [ResolvedTo(ANNOTATION_ARGUMENTS)] [PartialBodyAnalysisStateKey=1(1/2) #1] fun g(): R|kotlin/collections/List<kotlin/Int>| {
                        [ResolvedTo(BODY_RESOLVE)] lval e: R|kotlin/collections/List<T>| = Null(null)!!
                        ^g e#.myMap#(::f#)
                    }
                """.trimIndent(),
                renderTarget(targetFirFunction.fir),
            )

            val lastFirElement = executeOnPooledThreadInReadAction {
                psiStatements.last().getOrBuildFirOfType<FirElement>(resolutionFacade)
            }

            testServices.assertions.assertEquals(
                """
                        ^g R|<local>/e|.R|/myMap|<R|T|, R|kotlin/Int|>(::R|SubstitutionOverride</G.f: R|kotlin/Int|>|)
                    """.trimIndent(),
                renderTarget(lastFirElement),
            )
        }
    }

    @Test
    fun rootCompilerPluginAnnotation(ktFile: KtFile, testServices: TestServices) {
        withResolutionFacade(ktFile) { resolutionFacade ->
            val pluginAnnotations = resolutionFacade.useSiteFirSession.registeredPluginAnnotations
            val assertions = testServices.assertions
            assertions.assertTrue(pluginAnnotations is LLFirIdeRegisteredPluginAnnotations) {
                "The IDE implementation is expected"
            }

            assertions.assertEquals(
                expected = emptySet<FqName>(),
                actual = pluginAnnotations.getAnnotationsWithMetaAnnotation(FqName.ROOT),
            )
        }
    }
}
