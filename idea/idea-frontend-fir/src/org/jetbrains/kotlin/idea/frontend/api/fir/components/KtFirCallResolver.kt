/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirSafeCallExpression
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.calls.FirErrorReferenceWithCandidate
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.idea.fir.getCandidateSymbols
import org.jetbrains.kotlin.idea.fir.isImplicitFunctionCall
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFir
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFirSafe
import org.jetbrains.kotlin.idea.frontend.api.*
import org.jetbrains.kotlin.idea.frontend.api.calls.*
import org.jetbrains.kotlin.idea.frontend.api.components.KtCallResolver
import org.jetbrains.kotlin.idea.frontend.api.diagnostics.KtNonBoundToPsiErrorDiagnostic
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.buildSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.references.FirReferenceResolveHelper
import org.jetbrains.kotlin.idea.util.getElementTextInContext
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtElement

internal class KtFirCallResolver(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
) : KtCallResolver(), KtFirAnalysisSessionComponent {

    override fun resolveCall(call: KtBinaryExpression): KtCall? = withValidityAssertion {
        val firCall = call.getOrBuildFirSafe<FirFunctionCall>(firResolveState) ?: return null
        resolveCall(firCall)
    }

    override fun resolveCall(call: KtCallExpression): KtCall? = withValidityAssertion {
        val firCall = when (val fir = call.getOrBuildFir(firResolveState)) {
            is FirFunctionCall -> fir
            is FirSafeCallExpression -> fir.regularQualifiedAccess as? FirFunctionCall
            else -> null
        } ?: return null
        return resolveCall(firCall)
    }

    private fun resolveCall(firCall: FirFunctionCall): KtCall? {
        val session = firResolveState.rootModuleSession
        return when {
            firCall.isImplicitFunctionCall() -> {
                val target = with(FirReferenceResolveHelper) {
                    val calleeReference = (firCall.dispatchReceiver as FirQualifiedAccessExpression).calleeReference
                    calleeReference.toTargetSymbol(session, firSymbolBuilder).singleOrNull()
                }
                when (target) {
                    is KtVariableLikeSymbol -> firCall.createCallByVariableLikeSymbolCall(target)
                    null -> null
                    else -> firCall.asSimpleFunctionCall()
                }
            }
            else -> firCall.asSimpleFunctionCall()
        }
    }

    private fun FirFunctionCall.createCallByVariableLikeSymbolCall(variableLikeSymbol: KtVariableLikeSymbol) =
        when (val callReference = calleeReference) {
            is FirResolvedNamedReference -> {
                val functionSymbol = callReference.resolvedSymbol as? FirNamedFunctionSymbol
                when (functionSymbol?.callableId) {
                    null -> null
                    in kotlinFunctionInvokeCallableIds -> KtFunctionalTypeVariableCall(variableLikeSymbol)
                    else -> (callReference.resolvedSymbol.fir.buildSymbol(firSymbolBuilder) as? KtFunctionSymbol)
                        ?.let { KtVariableWithInvokeFunctionCall(variableLikeSymbol, KtSuccessCallTarget(it)) }
                }
            }
            is FirErrorNamedReference -> KtVariableWithInvokeFunctionCall(
                variableLikeSymbol,
                callReference.createErrorCallTarget()
            )
            else -> error("Unexpected call reference ${callReference::class.simpleName}")
        }

    private fun FirFunctionCall.asSimpleFunctionCall(): KtFunctionCall? {
        val target = when (val calleeReference = calleeReference) {
            is FirResolvedNamedReference -> calleeReference.getKtFunctionOrConstructorSymbol()?.let { KtSuccessCallTarget(it) }
            is FirErrorNamedReference -> calleeReference.createErrorCallTarget()
            is FirErrorReferenceWithCandidate -> calleeReference.createErrorCallTarget()
            is FirSimpleNamedReference ->
                error(
                    """
                      Looks like ${this::class.simpleName} && it calle reference ${calleeReference::class.simpleName} were not resolved to BODY_RESOLVE phase,
                      consider resolving it containing declaration before starting resolve calls
                      ${this.render()}
                      ${(this.psi as? KtElement)?.getElementTextInContext()}
                      """.trimIndent()
                )
            else -> error("Unexpected call reference ${calleeReference::class.simpleName}")
        } ?: return null
        return KtFunctionCall(target)
    }

    private fun FirErrorNamedReference.createErrorCallTarget(): KtErrorCallTarget =
        KtErrorCallTarget(
            getCandidateSymbols().mapNotNull { it.fir.buildSymbol(firSymbolBuilder) as? KtFunctionLikeSymbol },
            source?.let { diagnostic.asKtDiagnostic(it) } ?: KtNonBoundToPsiErrorDiagnostic(factoryName = null, diagnostic.reason, token)
        )

    private fun FirErrorReferenceWithCandidate.createErrorCallTarget(): KtErrorCallTarget =
        KtErrorCallTarget(
            getCandidateSymbols().mapNotNull { it.fir.buildSymbol(firSymbolBuilder) as? KtFunctionLikeSymbol },
            source?.let { diagnostic.asKtDiagnostic(it) } ?: KtNonBoundToPsiErrorDiagnostic(factoryName = null, diagnostic.reason, token)
        )

    private fun FirResolvedNamedReference.getKtFunctionOrConstructorSymbol(): KtFunctionLikeSymbol? =
        resolvedSymbol.fir.buildSymbol(firSymbolBuilder) as? KtFunctionLikeSymbol


    companion object {
        private val kotlinFunctionInvokeCallableIds = (0..23).flatMapTo(hashSetOf()) { arity ->
            listOf(
                CallableId(StandardNames.getFunctionClassId(arity), Name.identifier("invoke")),
                CallableId(StandardNames.getSuspendFunctionClassId(arity), Name.identifier("invoke"))
            )
        }
    }
}
