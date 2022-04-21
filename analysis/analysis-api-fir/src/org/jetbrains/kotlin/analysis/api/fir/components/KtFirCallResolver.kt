/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.calls.*
import org.jetbrains.kotlin.analysis.api.diagnostics.KtNonBoundToPsiErrorDiagnostic
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.getCandidateSymbols
import org.jetbrains.kotlin.analysis.api.fir.isInvokeFunction
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirArrayOfSymbolProvider.arrayOf
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirArrayOfSymbolProvider.arrayOfSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirArrayOfSymbolProvider.arrayTypeToArrayOfCall
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirFunctionSymbol
import org.jetbrains.kotlin.analysis.api.impl.barebone.parentOfType
import org.jetbrains.kotlin.analysis.api.impl.base.components.AbstractKtCallResolver
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolver.AllCandidatesResolver
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCall
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.calls.AbstractCandidate
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.resolve.createConeDiagnosticForCandidateWithError
import org.jetbrains.kotlin.fir.resolve.dfa.unwrapSmartcastExpression
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeDiagnosticWithCandidates
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeHiddenCandidateError
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.scopes.processOverriddenFunctions
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.KtPsiUtil.deparenthesize
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.toKtPsiSourceElement
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.util.OperatorNameConventions.EQUALS
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal class KtFirCallResolver(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
) : AbstractKtCallResolver(), KtFirAnalysisSessionComponent {
    private val diagnosticCache = mutableListOf<KtDiagnostic>()

    private val equalsSymbolInAny: FirNamedFunctionSymbol by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val session = analysisSession.rootModuleSession
        val scope = session.declaredMemberScope(session.builtinTypes.anyType.toRegularClassSymbol(session)!!)
        lateinit var result: FirNamedFunctionSymbol
        scope.processFunctionsByName(EQUALS) {
            result = it
        }
        result
    }

    override fun resolveCall(psi: KtElement): KtCallInfo? = withValidityAssertion {
        val ktCallInfos = getCallInfo(psi) { psiToResolve, resolveCalleeExpressionOfFunctionCall, resolveFragmentOfCall ->
            listOfNotNull(
                toKtCallInfo(
                    psiToResolve,
                    resolveCalleeExpressionOfFunctionCall,
                    resolveFragmentOfCall
                )
            )
        }
        check(ktCallInfos.size <= 1) { "Should only return 1 KtCallInfo" }
        return ktCallInfos.singleOrNull()
    }

    private inline fun <T> getCallInfo(
        psi: KtElement,
        getCallInfo: FirElement.(
            psiToResolve: KtElement,
            resolveCalleeExpressionOfFunctionCall: Boolean,
            resolveFragmentOfCall: Boolean
        ) -> List<T>
    ): List<T> {
        if (psi.isNotResolvable()) return emptyList()

        val containingCallExpressionForCalleeExpression = psi.getContainingCallExpressionForCalleeExpression()
        val containingBinaryExpressionForLhs = psi.getContainingBinaryExpressionForIncompleteLhs()
        val containingUnaryExpressionForIncOrDec = psi.getContainingUnaryIncOrDecExpression()
        val psiToResolve = containingCallExpressionForCalleeExpression
            ?: psi.getContainingDotQualifiedExpressionForSelectorExpression()
            ?: containingBinaryExpressionForLhs
            ?: containingUnaryExpressionForIncOrDec
            ?: psi
        val fir = psiToResolve.getOrBuildFir(analysisSession.firResolveState) ?: return emptyList()
        return fir.getCallInfo(
            psiToResolve,
            psiToResolve == containingCallExpressionForCalleeExpression,
            psiToResolve == containingBinaryExpressionForLhs || psiToResolve == containingUnaryExpressionForIncOrDec
        )
    }

    private fun FirElement.toKtCallInfo(
        psi: KtElement,
        resolveCalleeExpressionOfFunctionCall: Boolean,
        resolveFragmentOfCall: Boolean,
    ): KtCallInfo? {
        if (this is FirCheckNotNullCall)
            return KtSuccessCallInfo(KtCheckNotNullCall(token, argumentList.arguments.first().psi as KtExpression))
        if (resolveCalleeExpressionOfFunctionCall && this is FirImplicitInvokeCall) {
            // For implicit invoke, we resolve the calleeExpression of the CallExpression to the call that creates the receiver of this
            // implicit invoke call. For example,
            // ```
            // fun test(f: () -> Unit) {
            //   f() // calleeExpression `f` resolves to the local variable access, while `f()` resolves to the implicit `invoke` call.
            //       // This way `f` is also the explicit receiver of this implicit `invoke` call
            // }
            // ```
            return explicitReceiver?.toKtCallInfo(
                psi,
                resolveCalleeExpressionOfFunctionCall = false,
                resolveFragmentOfCall = resolveFragmentOfCall
            )
        }
        return when (this) {
            is FirResolvable -> {
                when (val calleeReference = calleeReference) {
                    is FirResolvedNamedReference -> {
                        val call = createKtCall(psi, this, null, resolveFragmentOfCall)
                            ?: error("expect `createKtCall` to succeed for resolvable case")
                        KtSuccessCallInfo(call)
                    }
                    is FirErrorNamedReference -> {
                        val diagnostic = calleeReference.diagnostic
                        val ktDiagnostic = (source?.let { diagnostic.asKtDiagnostic(it, psi.toKtPsiSourceElement(), diagnosticCache) }
                            ?: KtNonBoundToPsiErrorDiagnostic(factoryName = null, diagnostic.reason, token))

                        if (diagnostic is ConeHiddenCandidateError)
                            return KtErrorCallInfo(emptyList(), ktDiagnostic, token)

                        val candidateCalls = mutableListOf<KtCall>()
                        if (diagnostic is ConeDiagnosticWithCandidates) {
                            diagnostic.candidates.mapNotNullTo(candidateCalls) {
                                createKtCall(psi, this@toKtCallInfo, it, resolveFragmentOfCall)
                            }
                        } else {
                            candidateCalls.addIfNotNull(createKtCall(psi, this, null, resolveFragmentOfCall))
                        }
                        KtErrorCallInfo(candidateCalls, ktDiagnostic, token)
                    }
                    // Unresolved delegated constructor call is untransformed and end up as an `FirSuperReference`
                    is FirSuperReference -> {
                        val delegatedConstructorCall = this as? FirDelegatedConstructorCall ?: return null
                        val errorTypeRef = delegatedConstructorCall.constructedTypeRef as? FirErrorTypeRef ?: return null
                        val psiSource = psi.toKtPsiSourceElement()
                        val ktDiagnostic =
                            errorTypeRef.diagnostic.asKtDiagnostic(source ?: psiSource, psiSource, diagnosticCache) ?: return null
                        KtErrorCallInfo(emptyList(), ktDiagnostic, token)
                    }
                    else -> null
                }
            }
            is FirArrayOfCall -> toKtCallInfo()
            is FirComparisonExpression -> compareToCall.toKtCallInfo(
                psi,
                resolveCalleeExpressionOfFunctionCall,
                resolveFragmentOfCall
            )
            // FIR does not resolve to a symbol for equality calls.
            is FirEqualityOperatorCall -> toKtCallInfo(psi)
            is FirSafeCallExpression -> selector.toKtCallInfo(
                psi,
                resolveCalleeExpressionOfFunctionCall,
                resolveFragmentOfCall
            )
            else -> null
        }
    }

    /**
     * When resolving the calleeExpression of a `KtCallExpression`, we resolve the entire `KtCallExpression` instead. This way, the
     * corresponding FIR element is the `FirFunctionCall`, etc. Implicit invoke is then specially handled after obtaining the
     * `FirImplicitInvokeCall`.
     *
     * Note that, if the calleeExpression is already a KtCallExpression, then we don't do this because such a callExpression can be properly
     * resolved to the desired FIR element. That is, cases like `getHighLevelFunction()()` just works, where the both `KtCallExpression`
     * resolve to the desired FIR element.
     */
    private fun KtElement.getContainingCallExpressionForCalleeExpression(): KtCallExpression? {
        if (this !is KtExpression) return null
        val calleeExpression = deparenthesize(this) ?: return null
        if (calleeExpression is KtCallExpression) return null
        val callExpression = parentOfType<KtCallExpression>() ?: return null
        if (deparenthesize(callExpression.calleeExpression) != calleeExpression) return null
        return callExpression
    }

    /**
     * For `=` and compound access like `+=`, `-=`, `*=`, `/=`, `%=`, the LHS of the binary expression is not a complete call. Hence we
     * find the containing binary expression and resolve that instead.
     *
     * However, if, say, `+=` resolves to `plusAssign`, then the LHS is self-contained. In this case we do not return the containing binary
     * expression so that the FIR element corresponding to the LHS is used directly.
     */
    private fun KtElement.getContainingBinaryExpressionForIncompleteLhs(): KtBinaryExpression? {
        if (this !is KtExpression) return null
        val lhs = deparenthesize(this)
        val binaryExpression = parentOfType<KtBinaryExpression>() ?: return null
        if (deparenthesize(binaryExpression.left) != lhs || binaryExpression.operationToken !in KtTokens.ALL_ASSIGNMENTS) return null
        val firBinaryExpression = binaryExpression.getOrBuildFir(analysisSession.firResolveState)
        if (firBinaryExpression is FirFunctionCall) {
            if (firBinaryExpression.origin == FirFunctionCallOrigin.Operator &&
                firBinaryExpression.calleeReference.name in OperatorNameConventions.ASSIGNMENT_OPERATIONS
            ) {
                return null
            }
        }
        return binaryExpression
    }

    /**
     * For prefix and postfix `++` and `--`, the idea is the same because FIR represents it as several operations. For example, for `i++`,
     * if the input PSI is `i`, we instead resolve `i++` and extract the read part of this access for `i`.
     */
    private fun KtElement.getContainingUnaryIncOrDecExpression(): KtUnaryExpression? {
        if (this !is KtExpression) return null
        val baseExpression = deparenthesize(this)
        val unaryExpression = parentOfType<KtUnaryExpression>() ?: return null
        if (deparenthesize(unaryExpression.baseExpression) != baseExpression ||
            unaryExpression.operationToken !in KtTokens.INCREMENT_AND_DECREMENT
        ) return null
        return unaryExpression
    }

    /**
     * When resolving selector expression of a [KtDotQualifiedExpression], we instead resolve the containing qualified expression. This way
     * the corresponding FIR element is the `FirFunctionCall` or `FirPropertyAccessExpression`, etc.
     */
    private fun KtElement.getContainingDotQualifiedExpressionForSelectorExpression(): KtQualifiedExpression? {
        val parent = parent
        if (parent is KtDotQualifiedExpression && parent.selectorExpression == this) return parent
        if (parent is KtSafeQualifiedExpression && parent.selectorExpression == this) return parent
        return null
    }

    private fun createKtCall(
        psi: KtElement,
        fir: FirResolvable,
        candidate: AbstractCandidate?,
        resolveFragmentOfCall: Boolean
    ): KtCall? {
        val targetSymbol = candidate?.symbol
            ?: (fir.calleeReference as? FirResolvedNamedReference)?.resolvedSymbol
            ?: (fir.calleeReference as? FirNamedReference)?.candidateSymbol
            ?: return null
        if (targetSymbol !is FirCallableSymbol<*>) return null
        if (targetSymbol is FirErrorFunctionSymbol || targetSymbol is FirErrorPropertySymbol) return null
        val unsubstitutedKtSignature = targetSymbol.toKtSignature()

        handleCompoundAccessCall(psi, fir, resolveFragmentOfCall)?.let { return it }

        var firstArgIsExtensionReceiver = false
        var isImplicitInvoke = false

        // TODO: Ideally, we should get the substitutor from the candidate. But it seems there is no way to get the substitutor from the
        //  candidate, `Candidate.substitutor` is not complete. maybe we can carry over the final substitutor if it's available from
        //  body resolve phase?
        val substitutor =
            (fir as? FirQualifiedAccess)?.createSubstitutorFromTypeArguments(targetSymbol) ?: KtSubstitutor.Empty(token)

        fun createKtPartiallyAppliedSymbolForImplicitInvoke(
            dispatchReceiver: FirExpression,
            extensionReceiver: FirExpression,
            explicitReceiverKind: ExplicitReceiverKind
        ): KtPartiallyAppliedSymbol<KtCallableSymbol, KtSignature<KtCallableSymbol>> {
            isImplicitInvoke = true
            val explicitReceiverPsi = when (psi) {
                is KtQualifiedExpression -> (psi.selectorExpression as KtCallExpression).calleeExpression
                is KtCallExpression -> psi.calleeExpression
                else -> error("unexpected PSI $psi for FirImplicitInvokeCall")
            } ?: error("missing calleeExpression in PSI $psi for FirImplicitInvokeCall")
            // For implicit invoke, the explicit receiver is always set in FIR and this receiver is the variable or property that has
            // the `invoke` member function. In this case, we use the `calleeExpression` in the `KtCallExpression` as the PSI
            // representation of this receiver. Caller can then use this PSI for further call resolution, which is implemented by the
            // parameter `resolveCalleeExpressionOfFunctionCall` in `toKtCallInfo`.
            val explicitReceiverValue = KtExplicitReceiverValue(explicitReceiverPsi, false, token)

            // Specially handle @ExtensionFunctionType
            if (dispatchReceiver.typeRef.coneTypeSafe<ConeKotlinType>()?.isExtensionFunctionType == true) {
                firstArgIsExtensionReceiver = true
            }

            val dispatchReceiverValue: KtReceiverValue?
            val extensionReceiverValue: KtReceiverValue?
            if (explicitReceiverKind == ExplicitReceiverKind.DISPATCH_RECEIVER) {
                dispatchReceiverValue = explicitReceiverValue
                if (firstArgIsExtensionReceiver) {
                    extensionReceiverValue = (fir as FirFunctionCall).arguments.first().toKtReceiverValue()
                } else {
                    extensionReceiverValue = extensionReceiver.toKtReceiverValue()
                }
            } else {
                dispatchReceiverValue = dispatchReceiver.toKtReceiverValue()
                extensionReceiverValue = explicitReceiverValue
            }
            return KtPartiallyAppliedSymbol(
                unsubstitutedKtSignature.substitute(substitutor),
                dispatchReceiverValue,
                extensionReceiverValue,
            )
        }

        val partiallyAppliedSymbol = if (candidate != null) {
            if (fir is FirImplicitInvokeCall ||
                (fir.calleeOrCandidateName != OperatorNameConventions.INVOKE && targetSymbol.isInvokeFunction())
            ) {
                // Implicit invoke (e.g., `x()`) will have a different callee symbol (e.g., `x`) than the candidate (e.g., `invoke`).
                createKtPartiallyAppliedSymbolForImplicitInvoke(
                    candidate.dispatchReceiverValue?.receiverExpression ?: FirNoReceiverExpression,
                    candidate.chosenExtensionReceiverValue?.receiverExpression ?: FirNoReceiverExpression,
                    candidate.explicitReceiverKind
                )
            } else {
                KtPartiallyAppliedSymbol(
                    unsubstitutedKtSignature.substitute(substitutor),
                    candidate.dispatchReceiverValue?.receiverExpression?.toKtReceiverValue(),
                    candidate.chosenExtensionReceiverValue?.receiverExpression?.toKtReceiverValue(),
                )
            }
        } else if (fir is FirQualifiedAccess) {
            if (fir is FirImplicitInvokeCall) {
                val explicitReceiverKind = if (fir.explicitReceiver == fir.dispatchReceiver) {
                    ExplicitReceiverKind.DISPATCH_RECEIVER
                } else {
                    ExplicitReceiverKind.EXTENSION_RECEIVER
                }
                createKtPartiallyAppliedSymbolForImplicitInvoke(fir.dispatchReceiver, fir.extensionReceiver, explicitReceiverKind)
            } else {
                KtPartiallyAppliedSymbol(
                    unsubstitutedKtSignature.substitute(substitutor),
                    fir.dispatchReceiver.toKtReceiverValue(),
                    fir.extensionReceiver.toKtReceiverValue()
                )
            }
        } else {
            KtPartiallyAppliedSymbol(unsubstitutedKtSignature, _dispatchReceiver = null, _extensionReceiver = null)
        }

        return when (fir) {
            is FirAnnotationCall -> {
                if (unsubstitutedKtSignature.symbol !is KtConstructorSymbol) return null
                @Suppress("UNCHECKED_CAST") // safe because of the above check on targetKtSymbol
                KtAnnotationCall(
                    partiallyAppliedSymbol as KtPartiallyAppliedFunctionSymbol<KtConstructorSymbol>,
                    fir.createArgumentMapping(partiallyAppliedSymbol.signature as KtFunctionLikeSignature<*>)
                )
            }
            is FirDelegatedConstructorCall -> {
                if (unsubstitutedKtSignature.symbol !is KtConstructorSymbol) return null
                @Suppress("UNCHECKED_CAST") // safe because of the above check on targetKtSymbol
                KtDelegatedConstructorCall(
                    partiallyAppliedSymbol as KtPartiallyAppliedFunctionSymbol<KtConstructorSymbol>,
                    if (fir.isThis) KtDelegatedConstructorCall.Kind.THIS_CALL else KtDelegatedConstructorCall.Kind.SUPER_CALL,
                    fir.createArgumentMapping(partiallyAppliedSymbol.signature as KtFunctionLikeSignature<*>)
                )
            }
            is FirVariableAssignment -> {
                if (unsubstitutedKtSignature.symbol !is KtVariableLikeSymbol) return null
                val rhs = fir.rValue.psi as? KtExpression
                @Suppress("UNCHECKED_CAST") // safe because of the above check on targetKtSymbol
                KtSimpleVariableAccessCall(
                    partiallyAppliedSymbol as KtPartiallyAppliedVariableSymbol<KtVariableLikeSymbol>,
                    KtSimpleVariableAccess.Write(rhs)
                )
            }
            is FirPropertyAccessExpression -> {
                if (unsubstitutedKtSignature.symbol !is KtVariableLikeSymbol) return null
                @Suppress("UNCHECKED_CAST") // safe because of the above check on targetKtSymbol
                KtSimpleVariableAccessCall(
                    partiallyAppliedSymbol as KtPartiallyAppliedVariableSymbol<KtVariableLikeSymbol>,
                    KtSimpleVariableAccess.Read
                )
            }
            is FirFunctionCall -> {
                if (unsubstitutedKtSignature.symbol !is KtFunctionLikeSymbol) return null
                val argumentMapping = if (candidate is Candidate) {
                    candidate.argumentMapping
                } else {
                    fir.argumentMapping
                }
                val argumentMappingWithoutExtensionReceiver =
                    if (firstArgIsExtensionReceiver) {
                        argumentMapping?.entries?.drop(1)
                    } else {
                        argumentMapping?.entries
                    }
                @Suppress("UNCHECKED_CAST") // safe because of the above check on targetKtSymbol
                KtSimpleFunctionCall(
                    partiallyAppliedSymbol as KtPartiallyAppliedFunctionSymbol<KtFunctionLikeSymbol>,
                    argumentMappingWithoutExtensionReceiver
                        ?.createArgumentMapping(partiallyAppliedSymbol.signature as KtFunctionLikeSignature<*>)
                        ?: LinkedHashMap(),
                    isImplicitInvoke
                )
            }
            is FirExpressionWithSmartcast -> createKtCall(psi, fir.originalExpression, candidate, resolveFragmentOfCall)
            else -> null
        }
    }

    private fun handleCompoundAccessCall(psi: KtElement, fir: FirResolvable, resolveFragmentOfCall: Boolean): KtCall? {
        if (psi is KtBinaryExpression && psi.operationToken in KtTokens.AUGMENTED_ASSIGNMENTS) {
            val rightOperandPsi = deparenthesize(psi.right) ?: return null
            val leftOperandPsi = deparenthesize(psi.left) ?: return null
            val compoundAssignKind = psi.getCompoundAssignKind()

            // handle compound assignment with array access convention
            if (fir is FirFunctionCall && fir.calleeReference.name == OperatorNameConventions.SET && leftOperandPsi is KtArrayAccessExpression) {
                val (operationPartiallyAppliedSymbol, getPartiallyAppliedSymbol, setPartiallyAppliedSymbol) =
                    getOperationPartiallyAppliedSymbolsForCompoundArrayAssignment(fir, leftOperandPsi) ?: return null

                val getAccessArgumentMapping = LinkedHashMap<KtExpression, KtVariableLikeSignature<KtValueParameterSymbol>>().apply {
                    putAll(leftOperandPsi.indexExpressions.zip(getPartiallyAppliedSymbol.signature.valueParameters))
                }

                return if (resolveFragmentOfCall) {
                    KtSimpleFunctionCall(getPartiallyAppliedSymbol, getAccessArgumentMapping, false)
                } else {
                    KtCompoundArrayAccessCall(
                        KtCompoundAccess.CompoundAssign(operationPartiallyAppliedSymbol, compoundAssignKind, rightOperandPsi),
                        leftOperandPsi.indexExpressions,
                        getPartiallyAppliedSymbol,
                        setPartiallyAppliedSymbol
                    )
                }
            }

            // handle compound assignment with variable
            if (fir is FirVariableAssignment && (leftOperandPsi is KtDotQualifiedExpression ||
                        leftOperandPsi is KtNameReferenceExpression)
            ) {
                val variablePartiallyAppliedSymbol = fir.toPartiallyAppliedSymbol() ?: return null
                val operationPartiallyAppliedSymbol =
                    getOperationPartiallyAppliedSymbolsForCompoundVariableAccess(fir, leftOperandPsi) ?: return null
                return if (resolveFragmentOfCall) {
                    KtSimpleVariableAccessCall(variablePartiallyAppliedSymbol, KtSimpleVariableAccess.Read)
                } else {
                    KtCompoundVariableAccessCall(
                        variablePartiallyAppliedSymbol,
                        KtCompoundAccess.CompoundAssign(operationPartiallyAppliedSymbol, compoundAssignKind, rightOperandPsi),
                    )
                }
            }
        } else if (psi is KtUnaryExpression && psi.operationToken in KtTokens.INCREMENT_AND_DECREMENT) {
            val incDecPrecedence = when (psi) {
                is KtPostfixExpression -> KtCompoundAccess.IncOrDecOperation.Precedence.POSTFIX
                else -> KtCompoundAccess.IncOrDecOperation.Precedence.PREFIX
            }
            val incOrDecOperationKind = psi.getInOrDecOperationKind()
            val baseExpression = deparenthesize(psi.baseExpression)

            // handle inc/dec/ with array access convention
            if (fir is FirFunctionCall && fir.calleeReference.name == OperatorNameConventions.SET && baseExpression is KtArrayAccessExpression) {
                val (operationPartiallyAppliedSymbol, getPartiallyAppliedSymbol, setPartiallyAppliedSymbol) =
                    getOperationPartiallyAppliedSymbolsForIncOrDecOperation(fir, baseExpression, incDecPrecedence) ?: return null

                val getAccessArgumentMapping = LinkedHashMap<KtExpression, KtVariableLikeSignature<KtValueParameterSymbol>>().apply {
                    putAll(baseExpression.indexExpressions.zip(getPartiallyAppliedSymbol.signature.valueParameters))
                }
                return if (resolveFragmentOfCall) {
                    KtSimpleFunctionCall(getPartiallyAppliedSymbol, getAccessArgumentMapping, false)
                } else {
                    KtCompoundArrayAccessCall(
                        KtCompoundAccess.IncOrDecOperation(operationPartiallyAppliedSymbol, incOrDecOperationKind, incDecPrecedence),
                        baseExpression.indexExpressions,
                        getPartiallyAppliedSymbol,
                        setPartiallyAppliedSymbol
                    )
                }
            }

            // handle inc/dec/ with variable
            if (fir is FirVariableAssignment && (baseExpression is KtDotQualifiedExpression ||
                        baseExpression is KtNameReferenceExpression)
            ) {
                val variablePartiallyAppliedSymbol = fir.toPartiallyAppliedSymbol() ?: return null
                val operationPartiallyAppliedSymbol =
                    getOperationPartiallyAppliedSymbolsForCompoundVariableAccess(fir, baseExpression) ?: return null
                return if (resolveFragmentOfCall) {
                    KtSimpleVariableAccessCall(variablePartiallyAppliedSymbol, KtSimpleVariableAccess.Read)
                } else {
                    KtCompoundVariableAccessCall(
                        variablePartiallyAppliedSymbol,
                        KtCompoundAccess.IncOrDecOperation(operationPartiallyAppliedSymbol, incOrDecOperationKind, incDecPrecedence),
                    )
                }
            }
        }
        return null
    }

    private data class CompoundArrayAccessPartiallyAppliedSymbols(
        val operationPartiallyAppliedSymbol: KtPartiallyAppliedFunctionSymbol<KtFunctionSymbol>,
        val getPartiallyAppliedSymbol: KtPartiallyAppliedFunctionSymbol<KtFunctionSymbol>,
        val setPartiallyAppliedSymbol: KtPartiallyAppliedFunctionSymbol<KtFunctionSymbol>
    )

    private fun getOperationPartiallyAppliedSymbolsForCompoundArrayAssignment(
        fir: FirFunctionCall,
        arrayAccessExpression: KtArrayAccessExpression
    ): CompoundArrayAccessPartiallyAppliedSymbols? {
        // The last argument of `set` is the new value to be set. This value should be a call to the respective `plus`, `minus`,
        // `times`, `div`, or `rem` function.
        val operationCall = fir.arguments.lastOrNull() as? FirFunctionCall ?: return null

        // The explicit receiver in this case is a synthetic FirFunctionCall to `get`, which does not have a corresponding PSI. So
        // we use the `leftOperandPsi` as the supplement.
        val operationPartiallyAppliedSymbol = operationCall.toPartiallyAppliedSymbol(arrayAccessExpression) ?: return null

        // The explicit receiver for both `get` and `set` call should be the array expression.
        val getPartiallyAppliedSymbol =
            (operationCall.explicitReceiver as? FirFunctionCall)?.toPartiallyAppliedSymbol(arrayAccessExpression.arrayExpression)
                ?: return null
        val setPartiallyAppliedSymbol = fir.toPartiallyAppliedSymbol(arrayAccessExpression.arrayExpression) ?: return null

        return CompoundArrayAccessPartiallyAppliedSymbols(
            operationPartiallyAppliedSymbol,
            getPartiallyAppliedSymbol,
            setPartiallyAppliedSymbol
        )
    }

    @OptIn(SymbolInternals::class)
    private fun getOperationPartiallyAppliedSymbolsForIncOrDecOperation(
        fir: FirFunctionCall,
        arrayAccessExpression: KtArrayAccessExpression,
        incDecPrecedence: KtCompoundAccess.IncOrDecOperation.Precedence
    ): CompoundArrayAccessPartiallyAppliedSymbols? {
        val lastArg = fir.arguments.lastOrNull() ?: return null
        val setPartiallyAppliedSymbol = fir.toPartiallyAppliedSymbol(arrayAccessExpression.arrayExpression) ?: return null
        return when (incDecPrecedence) {
            KtCompoundAccess.IncOrDecOperation.Precedence.PREFIX -> {
                // For prefix case, the last argument is a reference to a synthetic local variable `<unary-result>` storing the result. So
                // we find the inc or dec operation call from the initializer of this local variable.
                val operationCall = getInitializerOfReferencedLocalVariable(lastArg) ?: return null
                val operationPartiallyAppliedSymbol = operationCall.toPartiallyAppliedSymbol(arrayAccessExpression) ?: return null
                // The get call is the explicit receiver of this operation call
                val getCall = operationCall.explicitReceiver as? FirFunctionCall ?: return null
                val getPartiallyAppliedSymbol = getCall.toPartiallyAppliedSymbol(arrayAccessExpression.arrayExpression) ?: return null
                CompoundArrayAccessPartiallyAppliedSymbols(
                    operationPartiallyAppliedSymbol,
                    getPartiallyAppliedSymbol,
                    setPartiallyAppliedSymbol
                )
            }
            KtCompoundAccess.IncOrDecOperation.Precedence.POSTFIX -> {
                // For postfix case, the last argument is the operation call invoked on a synthetic local variable `<unary>`. This local
                // variable is initialized by calling the `get` function.
                val operationCall = lastArg as? FirFunctionCall ?: return null
                val operationPartiallyAppliedSymbol = operationCall.toPartiallyAppliedSymbol(arrayAccessExpression) ?: return null
                val receiverOfOperationCall = operationCall.explicitReceiver ?: return null
                val getCall = getInitializerOfReferencedLocalVariable(receiverOfOperationCall)
                val getPartiallyAppliedSymbol = getCall?.toPartiallyAppliedSymbol(arrayAccessExpression.arrayExpression) ?: return null
                CompoundArrayAccessPartiallyAppliedSymbols(
                    operationPartiallyAppliedSymbol,
                    getPartiallyAppliedSymbol,
                    setPartiallyAppliedSymbol
                )
            }
        }
    }

    @OptIn(SymbolInternals::class)
    private fun getInitializerOfReferencedLocalVariable(variableReference: FirExpression): FirFunctionCall? {
        val qualifiedAccess = variableReference as? FirQualifiedAccess
        val resolvedNamedReference = qualifiedAccess?.calleeReference as? FirResolvedNamedReference
        val unaryResult = resolvedNamedReference?.resolvedSymbol as? FirVariableSymbol<*>
        return unaryResult?.fir?.initializer as? FirFunctionCall
    }

    private fun getOperationPartiallyAppliedSymbolsForCompoundVariableAccess(
        fir: FirVariableAssignment,
        leftOperandPsi: KtExpression
    ): KtPartiallyAppliedFunctionSymbol<KtFunctionSymbol>? {
        // The new value is a call to the appropriate operator function.
        val operationCall = fir.rValue as? FirFunctionCall ?: return null
        return operationCall.toPartiallyAppliedSymbol(leftOperandPsi)
    }

    private fun FirVariableAssignment.toPartiallyAppliedSymbol(): KtPartiallyAppliedVariableSymbol<KtVariableLikeSymbol>? {
        val variableRef = calleeReference as? FirResolvedNamedReference ?: return null
        val variableSymbol = variableRef.resolvedSymbol as? FirVariableSymbol<*> ?: return null
        val substitutor = createConeSubstitutorFromTypeArguments() ?: return null
        val ktSignature = variableSymbol.toKtSignature()
        return KtPartiallyAppliedSymbol(
            ktSignature.substitute(substitutor.toKtSubstitutor()),
            dispatchReceiver.toKtReceiverValue(),
            extensionReceiver.toKtReceiverValue(),
        )
    }

    private fun FirFunctionCall.toPartiallyAppliedSymbol(
        explicitReceiverPsiSupplement: KtExpression? = null
    ): KtPartiallyAppliedFunctionSymbol<KtFunctionSymbol>? {
        val operationSymbol =
            (calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirNamedFunctionSymbol ?: return null
        val substitutor = createConeSubstitutorFromTypeArguments() ?: return null
        val dispatchReceiverValue = if (explicitReceiverPsiSupplement != null && explicitReceiver == dispatchReceiver) {
            explicitReceiverPsiSupplement.toExplicitReceiverValue()
        } else {
            dispatchReceiver.toKtReceiverValue()
        }
        val extensionReceiverValue = if (explicitReceiverPsiSupplement != null && explicitReceiver == extensionReceiver) {
            explicitReceiverPsiSupplement.toExplicitReceiverValue()
        } else {
            extensionReceiver.toKtReceiverValue()
        }
        val ktSignature = operationSymbol.toKtSignature()
        return KtPartiallyAppliedSymbol(
            ktSignature.substitute(substitutor.toKtSubstitutor()),
            dispatchReceiverValue,
            extensionReceiverValue,
        )
    }

    @OptIn(SymbolInternals::class)
    private fun FirExpression.toKtReceiverValue(): KtReceiverValue? {
        val psi = psi
        return when (this) {
            is FirExpressionWithSmartcast -> {
                val result = originalExpression.toKtReceiverValue()
                if (result != null && isStable) {
                    KtSmartCastedReceiverValue(result, smartcastType.coneType.asKtType())
                } else {
                    result
                }
            }
            is FirThisReceiverExpression -> {
                if (psi == null) {
                    val implicitPartiallyAppliedSymbol = when (val partiallyAppliedSymbol = calleeReference.boundSymbol) {
                        is FirClassSymbol<*> -> partiallyAppliedSymbol.toKtSymbol()
                        is FirCallableSymbol<*> -> firSymbolBuilder.callableBuilder.buildExtensionReceiverSymbol(partiallyAppliedSymbol)
                            ?: return null
                        else -> return null
                    }
                    KtImplicitReceiverValue(implicitPartiallyAppliedSymbol)
                } else {
                    if (psi !is KtExpression) return null
                    psi.toExplicitReceiverValue()
                }
            }
            else -> {
                if (psi !is KtExpression) return null
                psi.toExplicitReceiverValue()
            }
        }
    }

    @OptIn(SymbolInternals::class)
    private fun FirCallableSymbol<*>.toKtSignature(): KtSignature<KtCallableSymbol> =
        firSymbolBuilder.callableBuilder.buildCallableSignature(this)

    @OptIn(SymbolInternals::class)
    private fun FirClassLikeSymbol<*>.toKtSymbol(): KtClassLikeSymbol = firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(this)

    @OptIn(SymbolInternals::class)
    private fun FirNamedFunctionSymbol.toKtSignature(): KtFunctionLikeSignature<KtFunctionSymbol> =
        firSymbolBuilder.functionLikeBuilder.buildFunctionSignature(this)

    @OptIn(SymbolInternals::class)
    private fun FirVariableSymbol<*>.toKtSignature(): KtVariableLikeSignature<KtVariableLikeSymbol> =
        firSymbolBuilder.variableLikeBuilder.buildVariableLikeSignature(this)

    override fun collectCallCandidates(psi: KtElement): List<KtCallCandidateInfo> = withValidityAssertion {
        getCallInfo(psi) { psiToResolve, resolveCalleeExpressionOfFunctionCall, resolveFragmentOfCall ->
            collectCallCandidates(
                psiToResolve,
                resolveCalleeExpressionOfFunctionCall,
                resolveFragmentOfCall
            )
        }
    }

    // TODO: Refactor common code with FirElement.toKtCallInfo() when other FirResolvables are handled
    private fun FirElement.collectCallCandidates(
        psi: KtElement,
        resolveCalleeExpressionOfFunctionCall: Boolean,
        resolveFragmentOfCall: Boolean,
    ): List<KtCallCandidateInfo> {
        if (this is FirCheckNotNullCall)
            return listOf(
                KtApplicableCallCandidateInfo(
                    KtCheckNotNullCall(token, argumentList.arguments.first().psi as KtExpression),
                    isInBestCandidates = true
                )
            )
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
                resolveFragmentOfCall = resolveFragmentOfCall
            ) ?: emptyList()
        }
        return when (this) {
            is FirFunctionCall -> collectCallCandidates(psi, resolveFragmentOfCall)
            is FirSafeCallExpression -> selector.collectCallCandidates(
                psi,
                resolveCalleeExpressionOfFunctionCall,
                resolveFragmentOfCall
            )
            is FirArrayOfCall, is FirComparisonExpression, is FirEqualityOperatorCall -> {
                toKtCallInfo(psi, resolveCalleeExpressionOfFunctionCall, resolveFragmentOfCall).toKtCallCandidateInfos()
            }
            else -> {
                // TODO: FirDelegatedConstructorCall, FirAnnotationCall, FirPropertyAccessExpression, FirVariableAssignment
                listOf()
            }
        }
    }

    private fun FirFunctionCall.collectCallCandidates(
        psi: KtElement,
        resolveFragmentOfCall: Boolean
    ): List<KtCallCandidateInfo> {
        // If a function call is resolved to an implicit invoke call, the FirImplicitInvokeCall will have the `invoke()` function as the
        // callee and the variable as the explicit receiver. To correctly get all candidates, we need to get the original function
        // call's explicit receiver (if there is any) and callee (i.e., the variable).
        val unwrappedExplicitReceiver = explicitReceiver?.unwrapSmartcastExpression()
        val originalFunctionCall =
            if (this is FirImplicitInvokeCall && unwrappedExplicitReceiver is FirPropertyAccessExpression) {
                val originalCallee = unwrappedExplicitReceiver.calleeReference.safeAs<FirNamedReference>() ?: return emptyList()
                buildFunctionCall {
                    // NOTE: We only need to copy the explicit receiver and not the dispatch and extension receivers as only the explicit
                    // receiver is needed by the resolver. The dispatch and extension receivers are only assigned after resolution when a
                    // candidate is selected.
                    source = this@collectCallCandidates.source
                    annotations.addAll(this@collectCallCandidates.annotations)
                    typeArguments.addAll(this@collectCallCandidates.typeArguments)
                    explicitReceiver = unwrappedExplicitReceiver.explicitReceiver
                    argumentList = this@collectCallCandidates.argumentList
                    calleeReference = originalCallee
                }
            } else {
                this
            }

        val calleeName = originalFunctionCall.calleeOrCandidateName ?: return emptyList()
        val candidates = AllCandidatesResolver(analysisSession.rootModuleSession).getAllCandidates(
            analysisSession.firResolveState,
            originalFunctionCall,
            calleeName,
            psi
        )
        return candidates.mapNotNull {
            convertToKtCallCandidateInfo(
                originalFunctionCall,
                psi,
                it.candidate,
                it.isInBestCandidates,
                resolveFragmentOfCall
            )
        }
    }

    private fun KtCallInfo?.toKtCallCandidateInfos(): List<KtCallCandidateInfo> {
        return when (this) {
            is KtSuccessCallInfo -> listOf(KtApplicableCallCandidateInfo(call, isInBestCandidates = true))
            is KtErrorCallInfo -> candidateCalls.map { KtInapplicableCallCandidateInfo(it, isInBestCandidates = true, diagnostic) }
            null -> emptyList()
        }
    }

    private fun convertToKtCallCandidateInfo(
        functionCall: FirFunctionCall,
        element: KtElement,
        candidate: Candidate,
        isInBestCandidates: Boolean,
        resolveFragmentOfCall: Boolean
    ): KtCallCandidateInfo? {
        val call = createKtCall(element, functionCall, candidate, resolveFragmentOfCall)
            ?: error("expect `createKtCall` to succeed for candidate")
        if (candidate.isSuccessful) {
            return KtApplicableCallCandidateInfo(call, isInBestCandidates)
        }

        val diagnostic = createConeDiagnosticForCandidateWithError(candidate.currentApplicability, candidate)
        if (diagnostic is ConeHiddenCandidateError) return null
        val ktDiagnostic =
            functionCall.source?.let { diagnostic.asKtDiagnostic(it, element.toKtPsiSourceElement(), diagnosticCache) }
                ?: KtNonBoundToPsiErrorDiagnostic(factoryName = null, diagnostic.reason, token)
        return KtInapplicableCallCandidateInfo(call, isInBestCandidates, ktDiagnostic)
    }

    private val FirResolvable.calleeOrCandidateName: Name?
        get() {
            val calleeReference = calleeReference
            if (calleeReference !is FirNamedReference) return null

            // In most cases, we can get the callee name from the callee's candidate symbols. However, there is at least one case where we
            // cannot do so:
            // ```
            // fun x(c: Char) {}
            // fun call(x: kotlin.Int) {
            //   operator fun Int.invoke(a: Int) {}
            //   operator fun Int.invoke(b: Boolean) {}
            //   <expr>x()</expr>
            // }
            // ```
            // The candidates for the call will both be `invoke`. We can keep it simple by getting the name from the callee reference's PSI
            // element (`x` in the above example) if possible.
            return when (val psi = calleeReference.psi) {
                is KtNameReferenceExpression -> psi.getReferencedNameAsName()
                else -> {
                    // This could be KtArrayAccessExpression or KtOperationReferenceExpression.
                    // Note: All candidate symbols should have the same name. We go by the symbol because `calleeReference.name` will include
                    // the applicability if not successful.
                    calleeReference.getCandidateSymbols().firstOrNull()?.safeAs<FirCallableSymbol<*>>()?.name
                }
            }
        }

    private fun FirArrayOfCall.toKtCallInfo(): KtCallInfo? {
        val arrayOfSymbol = with(analysisSession) {
            val type = typeRef.coneTypeSafe<ConeClassLikeType>()
                ?: return run {
                    val defaultArrayOfSymbol = arrayOfSymbol(arrayOf) ?: return null
                    val substitutor = createSubstitutorFromTypeArguments(defaultArrayOfSymbol)
                    KtErrorCallInfo(
                        listOf(
                            KtSimpleFunctionCall(
                                KtPartiallyAppliedSymbol(
                                    defaultArrayOfSymbol.toSignature(substitutor),
                                    null,
                                    null,
                                ),
                                createArgumentMapping(defaultArrayOfSymbol, substitutor),
                                false,
                            )
                        ),
                        KtNonBoundToPsiErrorDiagnostic(factoryName = null, "type of arrayOf call is not resolved", token),
                        token
                    )
                }
            val call = arrayTypeToArrayOfCall[type.lookupTag.classId] ?: arrayOf
            arrayOfSymbol(call)
        } ?: return null
        val substitutor = createSubstitutorFromTypeArguments(arrayOfSymbol)
        return KtSuccessCallInfo(
            KtSimpleFunctionCall(
                KtPartiallyAppliedSymbol(
                    arrayOfSymbol.toSignature(substitutor),
                    null,
                    null,
                ),
                createArgumentMapping(arrayOfSymbol, substitutor),
                false
            )
        )
    }

    private fun FirArrayOfCall.createSubstitutorFromTypeArguments(arrayOfSymbol: KtFirFunctionSymbol): KtSubstitutor {
        val firSymbol = arrayOfSymbol.firSymbol
        // No type parameter means this is an arrayOf call of primitives, in which case there is no type arguments
        val typeParameter = firSymbol.fir.typeParameters.singleOrNull() ?: return KtSubstitutor.Empty(token)
        val elementType = typeRef.coneTypeSafe<ConeClassLikeType>()?.arrayElementType() ?: return KtSubstitutor.Empty(token)
        val coneSubstitutor = substitutorByMap(mapOf(typeParameter.symbol to elementType), rootModuleSession)
        return firSymbolBuilder.typeBuilder.buildSubstitutor(coneSubstitutor)
    }


    private fun FirEqualityOperatorCall.toKtCallInfo(psi: KtElement): KtCallInfo? {
        val binaryExpression = deparenthesize(psi as? KtExpression) as? KtBinaryExpression ?: return null
        val leftPsi = binaryExpression.left ?: return null
        val rightPsi = binaryExpression.right ?: return null
        return when (operation) {
            FirOperation.EQ, FirOperation.NOT_EQ -> {
                val equalsSymbolInAny = equalsSymbolInAny
                val leftOperand = arguments.firstOrNull() ?: return null
                val session = analysisSession.rootModuleSession
                val classSymbol = leftOperand.typeRef.coneType.fullyExpandedType(session).toSymbol(session) as? FirClassSymbol<*>
                val equalsSymbol = classSymbol?.getEqualsSymbol(equalsSymbolInAny) ?: equalsSymbolInAny
                val ktSignature = equalsSymbol.toKtSignature()
                KtSuccessCallInfo(
                    KtSimpleFunctionCall(
                        KtPartiallyAppliedSymbol(
                            ktSignature,
                            KtExplicitReceiverValue(leftPsi, false, token),
                            null
                        ),
                        LinkedHashMap<KtExpression, KtVariableLikeSignature<KtValueParameterSymbol>>().apply {
                            put(rightPsi, ktSignature.valueParameters.first())
                        },
                        false
                    )
                )
            }
            else -> null
        }
    }

    private fun FirClassSymbol<*>.getEqualsSymbol(equalsSymbolInAny: FirNamedFunctionSymbol): FirNamedFunctionSymbol {
        val scope = unsubstitutedScope(
            analysisSession.rootModuleSession,
            analysisSession.getScopeSessionFor(analysisSession.rootModuleSession),
            false
        )
        var equalsSymbol: FirNamedFunctionSymbol? = null
        scope.processFunctionsByName(EQUALS) { equalsSymbolFromScope ->
            if (equalsSymbol != null) return@processFunctionsByName
            if (equalsSymbolFromScope == equalsSymbolInAny) {
                equalsSymbol = equalsSymbolFromScope
            }
            scope.processOverriddenFunctions(equalsSymbolFromScope) {
                if (it == equalsSymbolInAny) {
                    equalsSymbol = equalsSymbolFromScope
                    ProcessorAction.STOP
                } else {
                    ProcessorAction.NEXT
                }
            }
        }
        return equalsSymbol ?: equalsSymbolInAny
    }

    private fun FirCall.createArgumentMapping(signatureOfCallee: KtFunctionLikeSignature<*>): LinkedHashMap<KtExpression, KtVariableLikeSignature<KtValueParameterSymbol>> {
        return argumentMapping?.entries.createArgumentMapping(signatureOfCallee)
    }

    private fun Iterable<MutableMap.MutableEntry<FirExpression, FirValueParameter>>?.createArgumentMapping(
        signatureOfCallee: KtFunctionLikeSignature<*>
    ): LinkedHashMap<KtExpression, KtVariableLikeSignature<KtValueParameterSymbol>> {
        val paramSignatureByName = signatureOfCallee.valueParameters.associateBy {
            // We intentionally use `symbol.name` instead of `name` here, since
            // `FirValueParameter.name` is not affected by the `@ParameterName`
            it.symbol.name
        }
        val ktArgumentMapping = LinkedHashMap<KtExpression, KtVariableLikeSignature<KtValueParameterSymbol>>()
        this?.forEach { (firExpression, firValueParameter) ->
            val parameterSymbol = paramSignatureByName[firValueParameter.name] ?: return@forEach
            mapArgumentExpressionToParameter(firExpression, parameterSymbol, ktArgumentMapping)
        }
        return ktArgumentMapping
    }

    private fun FirArrayOfCall.createArgumentMapping(
        arrayOfCallSymbol: KtFirFunctionSymbol,
        substitutor: KtSubstitutor,
    ): LinkedHashMap<KtExpression, KtVariableLikeSignature<KtValueParameterSymbol>> {
        val ktArgumentMapping = LinkedHashMap<KtExpression, KtVariableLikeSignature<KtValueParameterSymbol>>()
        val parameterSymbol = arrayOfCallSymbol.valueParameters.single()

        for (firExpression in argumentList.arguments) {
            mapArgumentExpressionToParameter(firExpression, parameterSymbol.toSignature(substitutor), ktArgumentMapping)
        }
        return ktArgumentMapping
    }

    private fun mapArgumentExpressionToParameter(
        argumentExpression: FirExpression,
        parameterSymbol: KtVariableLikeSignature<KtValueParameterSymbol>,
        argumentMapping: LinkedHashMap<KtExpression, KtVariableLikeSignature<KtValueParameterSymbol>>
    ) {
        if (argumentExpression is FirVarargArgumentsExpression) {
            for (varargArgument in argumentExpression.arguments) {
                val valueArgument = varargArgument.findSourceKtExpressionForCallArgument() ?: return
                argumentMapping[valueArgument] = parameterSymbol
            }
        } else {
            val valueArgument = argumentExpression.findSourceKtExpressionForCallArgument() ?: return
            argumentMapping[valueArgument] = parameterSymbol
        }
    }

    private fun FirExpression.findSourceKtExpressionForCallArgument(): KtExpression? {
        // For spread, named, and lambda arguments, the source is the KtValueArgument.
        // For other arguments (including array indices), the source is the KtExpression.
        return when (this) {
            is FirNamedArgumentExpression, is FirSpreadArgumentExpression, is FirLambdaArgumentExpression ->
                realPsi.safeAs<KtValueArgument>()?.getArgumentExpression()
            else -> realPsi as? KtExpression
        }
    }
}
