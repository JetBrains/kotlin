/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.bridges

import org.jetbrains.kotlin.analysis.api.components.KaResolver
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
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
import org.jetbrains.kotlin.analysis.api.resolution.resolveCall as resolveCallEndpoint
import org.jetbrains.kotlin.analysis.api.resolution.resolveSymbol as resolveSymbolEndpoint
import org.jetbrains.kotlin.analysis.api.resolution.resolveSymbols as resolveSymbolsEndpoint
import org.jetbrains.kotlin.analysis.api.resolution.tryResolveCall as tryResolveCallEndpoint
import org.jetbrains.kotlin.analysis.api.resolution.tryResolveSymbols as tryResolveSymbolsEndpoint

/**
 * Routes the legacy [KaResolver] surface through the new public `context(session: KaSession)` resolution endpoints, which in turn reach the
 * [KaInternalsResolver] proxy. Members without a public endpoint (the legacy reference-based API) are forwarded straight to the proxy.
 */
@OptIn(KtExperimentalApi::class)
internal class KaResolverBridge(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaBaseSessionComponent<KaFirSession>(), KaResolver {
    private val proxy: KaInternalsResolver
        get() = analysisSession.resolver

    override fun KtResolvable.tryResolveSymbols(): KaSymbolResolutionAttempt? = context(analysisSession) { tryResolveSymbolsEndpoint() }

    override fun KtResolvable.resolveSymbols(): Collection<KaSymbol> = context(analysisSession) { resolveSymbolsEndpoint() }

    override fun KtResolvable.resolveSymbol(): KaSymbol? = context(analysisSession) { resolveSymbolEndpoint() }

    override fun KtAnnotationEntry.resolveSymbol(): KaConstructorSymbol? = context(analysisSession) { resolveSymbolEndpoint() }

    override fun KtSuperTypeCallEntry.resolveSymbol(): KaConstructorSymbol? = context(analysisSession) { resolveSymbolEndpoint() }

    override fun KtConstructorDelegationCall.resolveSymbol(): KaConstructorSymbol? = context(analysisSession) { resolveSymbolEndpoint() }

    override fun KtConstructorDelegationReferenceExpression.resolveSymbol(): KaConstructorSymbol? =
        context(analysisSession) { resolveSymbolEndpoint() }

    override fun KtCallElement.resolveSymbol(): KaFunctionSymbol? = context(analysisSession) { resolveSymbolEndpoint() }

    override fun KtCallableReferenceExpression.resolveSymbol(): KaCallableSymbol? = context(analysisSession) { resolveSymbolEndpoint() }

    override fun KtArrayAccessExpression.resolveSymbol(): KaNamedFunctionSymbol? = context(analysisSession) { resolveSymbolEndpoint() }

    override fun KtCollectionLiteralExpression.resolveSymbol(): KaNamedFunctionSymbol? =
        context(analysisSession) { resolveSymbolEndpoint() }

    override fun KtEnumEntrySuperclassReferenceExpression.resolveSymbol(): KaNamedClassSymbol? =
        context(analysisSession) { resolveSymbolEndpoint() }

    override fun KtLabelReferenceExpression.resolveSymbol(): KaDeclarationSymbol? = context(analysisSession) { resolveSymbolEndpoint() }

    override fun KtReturnExpression.resolveSymbol(): KaFunctionSymbol? = context(analysisSession) { resolveSymbolEndpoint() }

    override fun KtWhenConditionInRange.resolveSymbol(): KaNamedFunctionSymbol? = context(analysisSession) { resolveSymbolEndpoint() }

    override fun KtDestructuringDeclarationEntry.resolveSymbol(): KaCallableSymbol? = context(analysisSession) { resolveSymbolEndpoint() }

    override fun KtQualifiedExpression.resolveSymbol(): KaCallableSymbol? = context(analysisSession) { resolveSymbolEndpoint() }

    override fun KtConstructorCalleeExpression.resolveSymbol(): KaConstructorSymbol? = context(analysisSession) { resolveSymbolEndpoint() }

    override fun KtInstanceExpressionWithLabel.resolveSymbol(): KaDeclarationSymbol? = context(analysisSession) { resolveSymbolEndpoint() }

    override fun KtNullableType.resolveSymbol(): KaClassifierSymbol? = context(analysisSession) { resolveSymbolEndpoint() }

    override fun KtFunctionType.resolveSymbol(): KaClassSymbol? = context(analysisSession) { resolveSymbolEndpoint() }

    override fun KtTypeReference.resolveSymbol(): KaClassifierSymbol? = context(analysisSession) { resolveSymbolEndpoint() }

    override fun KtClassLiteralExpression.resolveSymbol(): KaClassifierSymbol? = context(analysisSession) { resolveSymbolEndpoint() }

    override fun KtSuperTypeEntry.resolveSymbol(): KaClassifierSymbol? = context(analysisSession) { resolveSymbolEndpoint() }

    override fun KtDelegatedSuperTypeEntry.resolveSymbol(): KaClassifierSymbol? = context(analysisSession) { resolveSymbolEndpoint() }

    override fun KtResolvableCall.tryResolveCall(): KaCallResolutionAttempt? = context(analysisSession) { tryResolveCallEndpoint() }

    override fun KtForExpression.tryResolveCall(): KaForLoopCallResolutionAttempt? = context(analysisSession) { tryResolveCallEndpoint() }

    override fun KtPropertyDelegate.tryResolveCall(): KaDelegatedPropertyCallResolutionAttempt? =
        context(analysisSession) { tryResolveCallEndpoint() }

    override fun KtResolvableCall.resolveCall(): KaSingleOrMultiCall? = context(analysisSession) { resolveCallEndpoint() }

    override fun KtAnnotationEntry.resolveCall(): KaAnnotationCall? = context(analysisSession) { resolveCallEndpoint() }

    override fun KtSuperTypeCallEntry.resolveCall(): KaFunctionCall<KaConstructorSymbol>? =
        context(analysisSession) { resolveCallEndpoint() }

    override fun KtConstructorDelegationCall.resolveCall(): KaDelegatedConstructorCall? = context(analysisSession) { resolveCallEndpoint() }

    override fun KtConstructorDelegationReferenceExpression.resolveCall(): KaDelegatedConstructorCall? =
        context(analysisSession) { resolveCallEndpoint() }

    override fun KtCallElement.resolveCall(): KaFunctionCall<*>? = context(analysisSession) { resolveCallEndpoint() }

    override fun KtCallableReferenceExpression.resolveCall(): KaCallableReferenceCall<*, *>? =
        context(analysisSession) { resolveCallEndpoint() }

    override fun KtArrayAccessExpression.resolveCall(): KaFunctionCall<KaNamedFunctionSymbol>? =
        context(analysisSession) { resolveCallEndpoint() }

    override fun KtCollectionLiteralExpression.resolveCall(): KaFunctionCall<KaNamedFunctionSymbol>? =
        context(analysisSession) { resolveCallEndpoint() }

    override fun KtEnumEntrySuperclassReferenceExpression.resolveCall(): KaDelegatedConstructorCall? =
        context(analysisSession) { resolveCallEndpoint() }

    override fun KtWhenConditionInRange.resolveCall(): KaFunctionCall<KaNamedFunctionSymbol>? =
        context(analysisSession) { resolveCallEndpoint() }

    override fun KtDestructuringDeclarationEntry.resolveCall(): KaSingleCall<*, *>? = context(analysisSession) { resolveCallEndpoint() }

    override fun KtQualifiedExpression.resolveCall(): KaSingleCall<*, *>? = context(analysisSession) { resolveCallEndpoint() }

    override fun KtForExpression.resolveCall(): KaForLoopCall? = context(analysisSession) { resolveCallEndpoint() }

    override fun KtPropertyDelegate.resolveCall(): KaDelegatedPropertyCall? = context(analysisSession) { resolveCallEndpoint() }

    override fun KtConstructorCalleeExpression.resolveCall(): KaFunctionCall<KaConstructorSymbol>? =
        context(analysisSession) { resolveCallEndpoint() }

    override fun KtNameReferenceExpression.resolveCall(): KaSingleCall<*, *>? = context(analysisSession) { resolveCallEndpoint() }

    override fun KtResolvableCall.collectCallCandidates(): List<KaCallCandidate> =
        context(analysisSession) { collectCallCandidatesEndpoint() }

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

    @Suppress("OVERRIDE_DEPRECATION")
    override val KtReference.usesContextSensitiveResolution: Boolean
        get() = proxy.usesContextSensitiveResolution(this)

    @Suppress("OVERRIDE_DEPRECATION")
    override val KtSimpleNameExpression.usesContextSensitiveResolution: Boolean
        get() = proxy.usesContextSensitiveResolution(this)
}
