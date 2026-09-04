/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.resolver

import com.intellij.psi.PsiElement
import com.intellij.psi.util.endOffset
import com.intellij.psi.util.startOffset
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaResolver
import org.jetbrains.kotlin.analysis.api.expressions.contextSensitiveResolutionStatus
import org.jetbrains.kotlin.analysis.api.expressions.isImplicitReferenceToCompanion
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.assertStableResult
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.findSpecializedResolveFunctions
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.stringRepresentation
import org.jetbrains.kotlin.analysis.api.resolution.*
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolution.KtResolvable
import org.jetbrains.kotlin.resolution.KtResolvableCall
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure

abstract class AbstractResolveSymbolTest : AbstractResolveByElementTest() {
    override val resolveKind: String get() = "symbol"

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.useDirectives(Directives)
    }

    open fun <R> analyzeSymbolElement(element: KtElement, testServices: TestServices, action: context(KaSession) () -> R): R {
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

        val localLookup = checkLookupLocally(mainElement, symbolAttempt, testServices)

        prettyPrint {
            if (mainElement is KtSimpleNameExpression) {
                appendLine("isImplicitReferenceToCompanion: ${mainElement.isImplicitReferenceToCompanion}")
                appendLine("contextSensitiveResolutionStatus: ${mainElement.contextSensitiveResolutionStatus}")
            }
            if (mainElement is KtNameReferenceExpression) {
                appendLine("lookupLocally: $localLookup")
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

    context(session: KaSession)
    private fun tryResolveSymbols(element: KtElement): KaSymbolResolutionAttempt? = if (element is KtResolvable) {
        element.tryResolveSymbols()
    } else {
        null
    }

    @OptIn(KtExperimentalApi::class)
    context(_: KaSession)
    private fun checkLookupLocally(
        mainElement: KtElement,
        symbolAttempt: KaSymbolResolutionAttempt?,
        testServices: TestServices,
    ): Boolean {
        if (mainElement !is KtNameReferenceExpression) return true

        val localLookup = mainElement.lookupLocally()
        val resolved = symbolAttempt?.successfulSymbols?.singleOrNull()?.psi

        val isSuppressed = Directives.IGNORE_LOOKUP_LOCALLY in testServices.moduleStructure.allDirectives
        val isConsistent = areEquivalent(localLookup, resolved)

        if (isSuppressed) {
            if (isConsistent) {
                testServices.assertions.fail {
                    "IGNORE_LOOKUP_LOCALLY was used, but the resolution was consistent. Remove the IGNORE_LOOKUP_LOCALLY directive."
                }
            }

            return localLookup != null
        }

        if (localLookup != null) {
            testServices.assertions.assertNotNull(resolved) {
                "${stringRepresentation(mainElement)} via lookupLocally resolved to ${stringRepresentation(localLookup)} which is not null, " +
                        "but symbol attempt is ${stringRepresentation(symbolAttempt)}"
            }
            ignoreStabilityIfNeeded {
                testServices.assertions.assertTrue(isConsistent) {
                    "${stringRepresentation(resolved)} != ${stringRepresentation(localLookup)}"
                }
            }
        }

        return localLookup != null
    }

    private object Directives : SimpleDirectivesContainer() {
        val RENDER_PSI_CLASS_NAME by directive(
            "Render also PSI class name for resolved symbols"
        )

        val IGNORE_LOOKUP_LOCALLY by directive(
            "Ignore the local lookup check"
        )
    }
}

/**
 * The function checks that all specific implementations of [KaResolver.resolveSymbol] are consistent.
 */
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
            null, is KaSimpleSymbolResolutionError -> assertions.assertEquals(expected = null, actual = specificCall)
            is KaSimpleSymbolResolutionSuccess -> {
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

private fun areEquivalent(e1: PsiElement?, e2: PsiElement?): Boolean {
    // work around default impl of PsiElementBase#isEquivalentTo, which currently has only a === check.
    // that is problematic because in our tests we create copies of the source file of the test, and sometimes we
    // can get different instances of the same logical element.
    //
    // This is just a workaround, we compare the position of the elements, and we check that the files they belong to match.
    if (e1 == null) return e2 == null
    if (e2 == null) return false

    if (e1.startOffset != e2.startOffset || e1.endOffset != e2.endOffset) return false

    val containingFile1 = e1.containingFile
    val containingFile2 = e2.containingFile

    if (containingFile1.isEquivalentTo(containingFile2)) return true
    if (containingFile1.originalFile.isEquivalentTo(containingFile2)) return true
    if (containingFile2.originalFile.isEquivalentTo(containingFile1)) return true
    return false
}
