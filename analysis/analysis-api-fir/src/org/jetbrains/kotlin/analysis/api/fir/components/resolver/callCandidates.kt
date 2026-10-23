/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.resolver

import org.jetbrains.kotlin.analysis.api.fir.components.*
import org.jetbrains.kotlin.analysis.api.fir.resolution.KaFirApplicableCallCandidate
import org.jetbrains.kotlin.analysis.api.fir.resolution.KaFirInapplicableCallCandidate
import org.jetbrains.kotlin.analysis.api.fir.utils.KaNonBoundToPsiErrorDiagnostic
import org.jetbrains.kotlin.analysis.api.resolution.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbolOfTypeSafe
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolver.AllCandidatesResolver
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCall
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate
import org.jetbrains.kotlin.fir.resolve.createConeDiagnosticForCandidateWithError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeHiddenCandidateError
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

// TODO: Refactor common code with FirElement.toKtCallInfo() when other FirResolvables are handled
context(resolver: KaFirResolver)
internal fun FirElement.collectCallCandidates(
    psi: KtElement,
    resolveCalleeExpressionOfFunctionCall: Boolean,
    resolveFragmentOfCall: Boolean,
): List<KaCallCandidate> {
    if (resolveCalleeExpressionOfFunctionCall && this is FirImplicitInvokeCall) {
        // For implicit invoke, we resolve the calleeExpression of the CallExpression to the call that creates the receiver of this
        // implicit invoke call. For example,
        // ```
        // fun test(f: () -> Unit) {
        //   f() // calleeExpression `f` resolves to the local variable access, while `f()` resolves to the implicit `invoke` call.
        //       // This way `f` is also the explicit receiver of this implicit `invoke` call
        // }
        // ```
        return explicitReceiver?.collectCallCandidates(
            psi,
            resolveCalleeExpressionOfFunctionCall = false,
            resolveFragmentOfCall = resolveFragmentOfCall,
        ) ?: emptyList()
    }

    return when (this) {
        is FirFunctionCall, is FirPropertyAccessExpression -> collectCallCandidates(psi, resolveFragmentOfCall)
        is FirSafeCallExpression -> unwrapSelector().collectCallCandidates(
            psi = psi,
            resolveCalleeExpressionOfFunctionCall = resolveCalleeExpressionOfFunctionCall,
            resolveFragmentOfCall = resolveFragmentOfCall,
        )

        is FirComparisonExpression -> compareToCall.collectCallCandidates(
            psi = psi,
            resolveCalleeExpressionOfFunctionCall = resolveCalleeExpressionOfFunctionCall,
            resolveFragmentOfCall = resolveFragmentOfCall,
        )

        is FirResolvedQualifier -> toKaCallCandidates()
        is FirDelegatedConstructorCall -> collectCallCandidatesForDelegatedConstructorCall(psi, resolveFragmentOfCall)
        else -> toKaResolutionAttempt(psi, resolveCalleeExpressionOfFunctionCall, resolveFragmentOfCall).toKaCallCandidates()
    }
}

context(component: KaFirSessionComponent)
private fun FirResolvedQualifier.toKaCallCandidates(): List<KaCallCandidate> {
    return toKaCalls(findQualifierConstructors()).map {
        KaFirInapplicableCallCandidate(
            backingCandidate = it,
            backingIsInBestCandidates = false,
            backingDiagnostic = inapplicableCandidateDiagnostic()
        )
    }
}

context(component: KaFirSessionComponent)
private fun FirQualifiedAccessExpression.collectCallCandidates(
    psi: KtElement,
    resolveFragmentOfCall: Boolean,
): List<KaCallCandidate> {
    // If a function call is resolved to an implicit invoke call, the FirImplicitInvokeCall will have the `invoke()` function as the
    // callee and the variable/qualifier as the explicit receiver. To correctly get all candidates, we need to get the original function
    // call's explicit receiver (if there is any) and callee (i.e., the variable).
    val unwrappedExplicitReceiver = explicitReceiver?.unwrapSmartcastExpression()?.takeIf {
        it is FirPropertyAccessExpression || it is FirResolvedQualifier
    }

    val isUnwrappedImplicitInvokeCall = this is FirImplicitInvokeCall && unwrappedExplicitReceiver != null
    val originalFunctionCall = if (isUnwrappedImplicitInvokeCall) {
        val originalExplicitReceiver = when (unwrappedExplicitReceiver) {
            is FirPropertyAccessExpression -> unwrappedExplicitReceiver.explicitReceiver
            is FirResolvedQualifier -> unwrappedExplicitReceiver.explicitParent
            else -> errorWithFirSpecificEntries(
                "Unsupported receiver type: ${unwrappedExplicitReceiver::class.simpleName}",
                fir = this,
            )
        }

        val originalCalleeReference = when (unwrappedExplicitReceiver) {
            is FirPropertyAccessExpression -> unwrappedExplicitReceiver.calleeReference
            is FirResolvedQualifier -> buildSimpleNamedReference {
                source = unwrappedExplicitReceiver.source
                name = unwrappedExplicitReceiver.relativeClassFqName?.shortName() ?: return emptyList()
            }
        }

        buildFunctionCall {
            // NOTE: We only need to copy the explicit receiver and not the dispatch and extension receivers as only the explicit
            // receiver is needed by the resolver. The dispatch and extension receivers are only assigned after resolution when a
            // candidate is selected.
            source = this@collectCallCandidates.source
            annotations.addAll(this@collectCallCandidates.annotations)
            typeArguments.addAll(this@collectCallCandidates.typeArguments)
            explicitReceiver = originalExplicitReceiver
            argumentList = this@collectCallCandidates.argumentList
            calleeReference = originalCalleeReference
        }
    } else {
        this
    }

    val calleeName = originalFunctionCall.calleeOrCandidateName ?: return emptyList()
    val candidates = AllCandidatesResolver(analysisSession.firSession).getAllCandidates(
        analysisSession.resolutionFacade,
        originalFunctionCall,
        calleeName,
        psi,
        ResolutionMode.ContextIndependent,
    )

    return candidates.mapNotNull {
        convertToKaCallCandidate(
            resolvable = originalFunctionCall,
            element = psi,
            candidate = it.candidate,
            isInBestCandidates = it.isInBestCandidates,
            resolveFragmentOfCall = resolveFragmentOfCall,
            isUnwrappedImplicitInvokeCall = isUnwrappedImplicitInvokeCall,
        )
    }
}

context(component: KaFirSessionComponent)
private fun FirDelegatedConstructorCall.collectCallCandidatesForDelegatedConstructorCall(
    psi: KtElement,
    resolveFragmentOfCall: Boolean,
): List<KaCallCandidate> {
    fun findDerivedClass(psi: KtElement): KtClassOrObject? {
        val parent = psi.parent
        return when (psi) {
            is KtConstructorDelegationCall -> (parent as? KtSecondaryConstructor)?.containingClassOrObject
            is KtSuperTypeCallEntry -> {
                (parent as? KtSuperTypeList)?.parent as? KtClassOrObject
                    ?: ((parent as? KtInitializerList)?.parent as? KtEnumEntry)?.containingClassOrObject
            }
            is KtConstructorCalleeExpression -> (parent as? KtElement)?.let(::findDerivedClass)
            is KtEnumEntrySuperclassReferenceExpression -> psi.getReferencedNameElement() as? KtClassOrObject
            else -> null
        }
    }

    val derivedClass = findDerivedClass(psi)?.resolveToFirSymbolOfTypeSafe<FirClassSymbol<*>>(resolutionFacade) ?: return emptyList()

    val candidates = AllCandidatesResolver(analysisSession.firSession)
        .getAllCandidatesForDelegatedConstructor(analysisSession.resolutionFacade, this, derivedClass, psi)

    return candidates.mapNotNull {
        convertToKaCallCandidate(
            resolvable = this,
            element = psi,
            candidate = it.candidate,
            isInBestCandidates = it.isInBestCandidates,
            resolveFragmentOfCall = resolveFragmentOfCall,
            isUnwrappedImplicitInvokeCall = false,
        )
    }
}

private fun KaCallResolutionAttempt?.toKaCallCandidates(): List<KaCallCandidate> = when (this) {
    is KaSimpleCallResolutionSuccess -> listOf(KaFirApplicableCallCandidate(backingCandidate = call, backingIsInBestCandidates = true))
    is KaSimpleCallResolutionError -> candidateCalls.map {
        KaFirInapplicableCallCandidate(
            backingCandidate = it,
            backingIsInBestCandidates = true,
            backingDiagnostic = diagnostic,
        )
    }

    // Unlike `fold`, the candidates of the successful sub-calls are collected as well
    is KaMultiCallResolutionAttempt -> successful?.let {
        listOf(KaFirApplicableCallCandidate(backingCandidate = it, backingIsInBestCandidates = true))
    } ?: simpleAttempts.flatMap { it.toKaCallCandidates() }

    null -> emptyList()
}

context(component: KaFirSessionComponent)
private fun convertToKaCallCandidate(
    resolvable: FirResolvable,
    element: KtElement,
    candidate: Candidate,
    isInBestCandidates: Boolean,
    resolveFragmentOfCall: Boolean,
    isUnwrappedImplicitInvokeCall: Boolean,
): KaCallCandidate? {
    val call = createKaCall(element, resolvable, candidate, resolveFragmentOfCall) ?: return null

    if (candidate.isSuccessful) {
        return KaFirApplicableCallCandidate(
            backingCandidate = call,
            backingIsInBestCandidates = if (isUnwrappedImplicitInvokeCall) {
                call is KaImplicitInvokeCall
            } else {
                isInBestCandidates
            }
        )
    }

    val diagnostic = createConeDiagnosticForCandidateWithError(candidate.lowestApplicability, candidate)
    if (diagnostic is ConeHiddenCandidateError) return null
    val kaDiagnostic = resolvable.source?.let { diagnostic.asKaDiagnostic(it) }
        ?: KaNonBoundToPsiErrorDiagnostic(factoryName = FirErrors.OTHER_ERROR.name, diagnostic.reason, component.token)

    return KaFirInapplicableCallCandidate(
        backingCandidate = call,
        backingIsInBestCandidates = isInBestCandidates,
        backingDiagnostic = kaDiagnostic,
    )
}
