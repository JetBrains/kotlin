/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.resolver

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaResolver
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.assertStableResult
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.findSpecializedResolveFunctions
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.stringRepresentation
import org.jetbrains.kotlin.analysis.api.resolution.*
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExperimentalApi
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolution.KtResolvable
import org.jetbrains.kotlin.resolution.KtResolvableCall
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure

@OptIn(KtExperimentalApi::class)
abstract class AbstractResolveSymbolTest : AbstractResolveByElementTest() {
    override val resolveKind: String get() = "symbol"

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.useDirectives(Directives)
    }

    open fun <R> analyzeSymbolElement(element: KtElement, testServices: TestServices, action: KaSession.() -> R): R {
        return analyzeForTest(element, action)
    }

    override fun generateResolveOutput(
        mainElement: KtElement,
        testServices: TestServices,
    ): String = analyzeSymbolElement(mainElement, testServices) {
        val symbolAttempt = tryResolveSymbols(mainElement)
        val secondSymbolAttempt = tryResolveSymbols(mainElement)

        ignoreStabilityIfNeeded {
            assertStableResult(testServices, symbolAttempt, secondSymbolAttempt)

            if (mainElement is KtResolvableCall) {
                val callAttempt = mainElement.tryResolveCall()
                assertStableResult(mainElement, testServices, symbolAttempt, callAttempt)
            }
        }

        // This call mustn't be suppressed as this is the API contracts
        if (mainElement is KtResolvable) {
            assertSpecificResolutionApi(testServices, symbolAttempt, mainElement)
        }

        prettyPrint {
            if (mainElement is KtSimpleNameExpression) {
                appendLine("isImplicitReferenceToCompanion: ${mainElement.isImplicitReferenceToCompanion}")
                appendLine("usesContextSensitiveResolution: ${mainElement.usesContextSensitiveResolution}")
            }

            val representation = stringRepresentation(symbolAttempt)
            append(representation)

            if (Directives.RENDER_PSI_CLASS_NAME in testServices.moduleStructure.allDirectives) {
                val symbols = symbolAttempt?.symbols.orEmpty()
                printCollectionIfNotEmpty(symbols, prefix = "\nPSI class names: ") { symbol ->
                    append(symbol.psi?.let { it::class.simpleName }.toString())
                }
            }

            val additionalInfo = symbolAttempt?.let { additionalSymbolInfo(it) }
            if (additionalInfo != null) {
                appendLine()
                append("additional: ")
                withIndent {
                    append(additionalInfo)
                }
            }
        }
    }

    context(session: KaSession)
    open fun additionalSymbolInfo(attempt: KaSymbolResolutionAttempt): String? = null

    private fun KaSession.tryResolveSymbols(element: KtElement): KaSymbolResolutionAttempt? = if (element is KtResolvable) {
        element.tryResolveSymbols()
    } else {
        null
    }

    private object Directives : SimpleDirectivesContainer() {
        val RENDER_PSI_CLASS_NAME by directive(
            "Render also PSI class name for resolved symbols"
        )
    }
}

/**
 * The function checks that all specific implementations of [KaResolver.resolveSymbol] are consistent.
 */
@OptIn(KtExperimentalApi::class)
context(session: KaSession)
internal fun assertSpecificResolutionApi(
    testServices: TestServices,
    attempt: KaSymbolResolutionAttempt?,
    element: KtResolvable,
) {
    val elementClass = element::class

    val assertions = testServices.assertions
    for (kFunction in KaResolver::class.findSpecializedResolveFunctions("resolveSymbol", elementClass)) {
        val specificCall = kFunction.call(session, element)

        when (attempt) {
            null, is KaSymbolResolutionError -> assertions.assertEquals(expected = null, actual = specificCall)
            is KaSymbolResolutionSuccess -> {
                // Only non-compound cases can be checked
                assertions.assertEquals(expected = attempt.symbols.singleOrNull(), actual = specificCall)
            }
            is KaCompoundSymbolResolutionError -> {
                // Multi-symbol resolution: specialized resolveSymbol returns null for compound cases
                assertions.assertEquals(expected = null, actual = specificCall)
            }
        }
    }
}
