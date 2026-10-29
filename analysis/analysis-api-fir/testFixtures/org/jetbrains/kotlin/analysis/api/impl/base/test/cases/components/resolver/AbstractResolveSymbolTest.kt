/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.resolver

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaResolver
import org.jetbrains.kotlin.analysis.api.expressions.contextSensitiveResolutionStatus
import org.jetbrains.kotlin.analysis.api.expressions.isImplicitReferenceToCompanion
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.assertStableResult
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.findSpecializedResolveFunctions
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.stringRepresentation
import org.jetbrains.kotlin.analysis.api.projectStructure.isDangling
import org.jetbrains.kotlin.analysis.api.resolution.*
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.isLocal
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExperimentalApi
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.LookupLocallyInternals
import org.jetbrains.kotlin.psi.lookupLocally
import org.jetbrains.kotlin.resolution.KtResolvable
import org.jetbrains.kotlin.resolution.KtResolvableCall
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure

abstract class AbstractResolveSymbolTest : AbstractResolveByElementTest() {
    override val resolveKinds: List<ResolveKind> get() = listOf(ResolveKind.SYMBOL)
}

context(_: KaSession)
internal fun AbstractResolveByElementTest.generateSymbolOutput(mainElement: KtElement, testServices: TestServices): String {
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

    return prettyPrint {
        if (mainElement is KtSimpleNameExpression) {
            appendLine("isImplicitReferenceToCompanion: ${mainElement.isImplicitReferenceToCompanion}")
            appendLine("contextSensitiveResolutionStatus: ${mainElement.contextSensitiveResolutionStatus}")
            if (mainElement is KtNameReferenceExpression) {
                appendLine("lookupLocally: $localLookup")
            }
        }

        val representation = stringRepresentation(symbolAttempt)
        append(representation)

        if (ResolveSymbolTestDirectives.RENDER_PSI_CLASS_NAME in testServices.moduleStructure.allDirectives) {
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
private fun tryResolveSymbols(element: KtElement): KaSymbolResolutionAttempt? = if (element is KtResolvable) {
    element.tryResolveSymbols()
} else {
    null
}

internal object ResolveSymbolTestDirectives : SimpleDirectivesContainer() {
    val RENDER_PSI_CLASS_NAME by directive(
        "Render also PSI class name for resolved symbols"
    )

    val IGNORE_LOOKUP_LOCALLY by directive(
        "Ignore the local lookup check"
    )
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

@OptIn(KtExperimentalApi::class)
context(_: KaSession)
private fun AbstractResolveByElementTest.checkLookupLocally(
    mainElement: KtElement,
    symbolAttempt: KaSymbolResolutionAttempt?,
    testServices: TestServices,
): Boolean {
    if (mainElement !is KtNameReferenceExpression) return true

    val localLookup = mainElement.lookupLocally()
    val resolved = symbolAttempt?.successfulSymbols?.singleOrNull()?.psi

    val isSuppressed = ResolveSymbolTestDirectives.IGNORE_LOOKUP_LOCALLY in testServices.moduleStructure.allDirectives
    val isConsistent = when {
        localLookup == null -> resolved == null
        else -> localLookup.isEquivalentTo(resolved)
    }

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
    } else {
        // completeness: localLookup can resolve any local symbols, except those that are explicitly not resolvable
        // see `canPerformLocalLookup`
        val resolved = symbolAttempt?.successfulSymbols?.singleOrNull()

        @OptIn(KtImplementationDetail::class)
        val localLookupContextKind = LookupLocallyInternals.getContextKind(mainElement)
        @OptIn(KtImplementationDetail::class)
        if (resolved != null && resolved.isLocal && localLookupContextKind != null) {
            testServices.assertions.assertFalse(
                resolved.canPerformLocalLookup(localLookupContextKind, mainElement)
            ) {
                "Should be able to resolve ${stringRepresentation(mainElement)} to ${stringRepresentation(resolved)} via lookupLocally"
            }
        }
    }

    return localLookup != null
}

@OptIn(KtImplementationDetail::class)
context(_: KaSession)
private fun KaSymbol.canPerformLocalLookup(
    localLookupContextKind: LookupLocallyInternals.ContextKind,
    startingReference: KtNameReferenceExpression,
): Boolean {
    if (!isLocal) return false
    if (startingReference.containingKtFile.isDangling) return false
    if (!LookupLocallyInternals.canPerformLocalLookup(startingReference)) return false

    fun crossesNonLocalBoundary(): Boolean {
        val commonParent = PsiTreeUtil.findCommonParent(startingReference, this.psi)
        return generateSequence<PsiElement>(startingReference) { it.parent }
            .takeWhile {
                val p = commonParent?.parent
                if (p != null) it != p else true
            }
            .any {
                it is KtNamedFunction && !it.isLocal
                        || it is KtProperty && !it.isLocal
                        || it is KtClassOrObject
            }
    }

    when (this) {
        is KaValueParameterSymbol -> {
            if (isImplicitLambdaParameter) {
                // we need BODY_RESOLVE to resolve implicit lambda parameters
                return false
            }
        }
        is KaClassSymbol -> {
            if (localLookupContextKind == LookupLocallyInternals.ContextKind.VALUE) {
                // object A
                // fun f() {
                //     class A
                //     A
                //     ^ name in value context, we cannot resolve it to the local class as we
                //       should pick the `object A` outside of the function.
                // }
                return false
            }
        }
        is KaTypeParameterSymbol -> {
            if (crossesNonLocalBoundary()) {
                // we cannot resolve type parameters across non-local boundaries
                //
                // class X<T> {
                //     inner class Y: T()
                //                    ^ we need to let the compiler resolve this T
                // }
                //
                // or
                //
                // val <T> T.x: Any get() = object {
                //     val x: Int get() = emptyList<T>().size
                //                                  ^ cannot figure out what T refers to here, as it crosses a non-local boundary
                // }
                return false
            }
        }
    }

    return true
}
