/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package org.jetbrains.kotlin.analysis.api.fir.components.bridges

import org.jetbrains.kotlin.analysis.api.components.KaResolver
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.components.KaFirSessionComponent
import org.jetbrains.kotlin.analysis.api.internals.KaInternalsResolver
import org.jetbrains.kotlin.analysis.api.resolution.*
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolution.KtResolvable
import org.jetbrains.kotlin.resolution.KtResolvableCall
import org.jetbrains.kotlin.analysis.api.expressions.contextSensitiveResolutionStatus as contextSensitiveResolutionStatusEndpoint
import org.jetbrains.kotlin.analysis.api.expressions.isImplicitReferenceToCompanion as isImplicitReferenceToCompanionEndpoint
import org.jetbrains.kotlin.analysis.api.resolution.collectCallCandidates as collectCallCandidatesEndpoint
import org.jetbrains.kotlin.analysis.api.resolution.tryResolveCall as tryResolveCallEndpoint
import org.jetbrains.kotlin.analysis.api.resolution.tryResolveSymbols as tryResolveSymbolsEndpoint

/**
 * Routes the legacy [KaResolver] surface through the new public `context(session: KaSession)` resolution endpoints, which in turn reach the
 * [KaInternalsResolver] proxy. Members without a public endpoint (the legacy reference-based API) are forwarded straight to the proxy.
 */
internal class KaResolverBridge(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaFirSessionComponent(), KaResolver {
    private val proxy: KaInternalsResolver
        get() = analysisSession.resolver

    override fun KtResolvable.tryResolveSymbols(): KaSymbolResolutionAttempt? = tryResolveSymbolsEndpoint(session = analysisSession)

    override fun KtResolvable.resolveSymbols(): Collection<KaSymbol> = resolveSuccessfulSymbols(session = analysisSession)

    override fun KtResolvable.resolveSymbol(): KaSymbol? = resolveSuccessfulSymbol(session = analysisSession)

    override fun KtAnnotationEntry.resolveSymbol(): KaConstructorSymbol? = resolveSuccessfulSymbol(session = analysisSession)

    override fun KtSuperTypeCallEntry.resolveSymbol(): KaConstructorSymbol? = resolveSuccessfulSymbol(session = analysisSession)

    override fun KtConstructorDelegationCall.resolveSymbol(): KaConstructorSymbol? = resolveSuccessfulSymbol(session = analysisSession)

    override fun KtConstructorDelegationReferenceExpression.resolveSymbol(): KaConstructorSymbol? =
        resolveSuccessfulSymbol(session = analysisSession)

    override fun KtCallElement.resolveSymbol(): KaFunctionSymbol? = resolveSuccessfulSymbol(session = analysisSession)

    override fun KtCallableReferenceExpression.resolveSymbol(): KaCallableSymbol? = resolveSuccessfulSymbol(session = analysisSession)

    override fun KtArrayAccessExpression.resolveSymbol(): KaNamedFunctionSymbol? = resolveSuccessfulSymbol(session = analysisSession)

    override fun KtCollectionLiteralExpression.resolveSymbol(): KaNamedFunctionSymbol? =
        resolveSuccessfulSymbol(session = analysisSession)

    override fun KtEnumEntrySuperclassReferenceExpression.resolveSymbol(): KaNamedClassSymbol? =
        resolveSuccessfulSymbol(session = analysisSession)

    override fun KtLabelReferenceExpression.resolveSymbol(): KaDeclarationSymbol? = resolveSuccessfulSymbol(session = analysisSession)

    override fun KtReturnExpression.resolveSymbol(): KaFunctionSymbol? = resolveSuccessfulSymbol(session = analysisSession)

    override fun KtWhenConditionInRange.resolveSymbol(): KaNamedFunctionSymbol? = resolveSuccessfulSymbol(session = analysisSession)

    override fun KtDestructuringDeclarationEntry.resolveSymbol(): KaCallableSymbol? = resolveSuccessfulSymbol(session = analysisSession)

    override fun KtQualifiedExpression.resolveSymbol(): KaCallableSymbol? = resolveSuccessfulSymbol(session = analysisSession)

    override fun KtConstructorCalleeExpression.resolveSymbol(): KaConstructorSymbol? = resolveSuccessfulSymbol(session = analysisSession)

    override fun KtInstanceExpressionWithLabel.resolveSymbol(): KaDeclarationSymbol? = resolveSuccessfulSymbol(session = analysisSession)

    override fun KtNullableType.resolveSymbol(): KaClassifierSymbol? = resolveSuccessfulSymbol(session = analysisSession)

    override fun KtFunctionType.resolveSymbol(): KaClassSymbol? = resolveSuccessfulSymbol(session = analysisSession)

    override fun KtTypeReference.resolveSymbol(): KaClassifierSymbol? = resolveSuccessfulSymbol(session = analysisSession)

    override fun KtClassLiteralExpression.resolveSymbol(): KaClassifierSymbol? = resolveSuccessfulSymbol(session = analysisSession)

    override fun KtSuperTypeEntry.resolveSymbol(): KaClassifierSymbol? = resolveSuccessfulSymbol(session = analysisSession)

    override fun KtDelegatedSuperTypeEntry.resolveSymbol(): KaClassifierSymbol? = resolveSuccessfulSymbol(session = analysisSession)

    override fun KtResolvableCall.tryResolveCall(): KaCallResolutionAttempt? = tryResolveCallEndpoint(session = analysisSession)

    override fun KtForExpression.tryResolveCall(): KaForLoopCallResolutionAttempt? = tryResolveCallEndpoint(session = analysisSession)

    override fun KtPropertyDelegate.tryResolveCall(): KaDelegatedPropertyCallResolutionAttempt? =
        tryResolveCallEndpoint(session = analysisSession)

    override fun KtResolvableCall.resolveCall(): KaSimpleOrMultiCall? = resolveSuccessfulCall(session = analysisSession)

    override fun KtAnnotationEntry.resolveCall(): KaAnnotationCall? = resolveSuccessfulCall(session = analysisSession)

    override fun KtSuperTypeCallEntry.resolveCall(): KaFunctionCall<KaConstructorSymbol>? =
        resolveSuccessfulCall(session = analysisSession)

    override fun KtConstructorDelegationCall.resolveCall(): KaDelegatedConstructorCall? = resolveSuccessfulCall(session = analysisSession)

    override fun KtConstructorDelegationReferenceExpression.resolveCall(): KaDelegatedConstructorCall? =
        resolveSuccessfulCall(session = analysisSession)

    override fun KtCallElement.resolveCall(): KaFunctionCall<*>? = resolveSuccessfulCall(session = analysisSession)

    override fun KtCallableReferenceExpression.resolveCall(): KaCallableReferenceCall<*, *>? =
        resolveSuccessfulCall(session = analysisSession)

    override fun KtArrayAccessExpression.resolveCall(): KaFunctionCall<KaNamedFunctionSymbol>? =
        resolveSuccessfulCall(session = analysisSession)

    override fun KtCollectionLiteralExpression.resolveCall(): KaFunctionCall<KaNamedFunctionSymbol>? =
        resolveSuccessfulCall(session = analysisSession)

    override fun KtEnumEntrySuperclassReferenceExpression.resolveCall(): KaDelegatedConstructorCall? =
        resolveSuccessfulCall(session = analysisSession)

    override fun KtWhenConditionInRange.resolveCall(): KaFunctionCall<KaNamedFunctionSymbol>? =
        resolveSuccessfulCall(session = analysisSession)

    override fun KtDestructuringDeclarationEntry.resolveCall(): KaSimpleCall<*, *>? = resolveSuccessfulCall(session = analysisSession)

    override fun KtQualifiedExpression.resolveCall(): KaSimpleCall<*, *>? = resolveSuccessfulCall(session = analysisSession)

    override fun KtForExpression.resolveCall(): KaForLoopCall? = resolveSuccessfulCall(session = analysisSession)

    override fun KtPropertyDelegate.resolveCall(): KaDelegatedPropertyCall? = resolveSuccessfulCall(session = analysisSession)

    override fun KtConstructorCalleeExpression.resolveCall(): KaFunctionCall<KaConstructorSymbol>? =
        resolveSuccessfulCall(session = analysisSession)

    override fun KtNameReferenceExpression.resolveCall(): KaSimpleCall<*, *>? = resolveSuccessfulCall(session = analysisSession)

    override fun KtResolvableCall.collectCallCandidates(): List<KaCallCandidate> =
        collectCallCandidatesEndpoint(session = analysisSession)

    override fun KtElement.resolveToCall(): KaCallInfo? = proxy.resolveToCall(this)

    override fun KtElement.resolveToCallCandidates(): List<KaCallCandidateInfo> = proxy.resolveToCallCandidates(this)

    override val KtSimpleNameExpression.isImplicitReferenceToCompanion: Boolean
        get() = context(analysisSession) { isImplicitReferenceToCompanionEndpoint }

    override val KtSimpleNameExpression.contextSensitiveResolutionStatus: KaContextSensitiveResolutionStatus
        get() = context(analysisSession) { contextSensitiveResolutionStatusEndpoint }

    override fun KtReference.resolveToSymbols(): Collection<KaSymbol> = proxy.resolveToSymbols(this)

    override fun KtReference.resolveToSymbol(): KaSymbol? = proxy.resolveToSymbol(this)

    @Suppress("OVERRIDE_DEPRECATION")
    override fun KtReference.isImplicitReferenceToCompanion(): Boolean = proxy.isImplicitReferenceToCompanion(this)
}
