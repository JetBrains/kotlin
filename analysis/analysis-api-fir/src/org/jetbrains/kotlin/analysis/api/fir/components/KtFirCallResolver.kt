/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.calls.*
import org.jetbrains.kotlin.analysis.api.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.analysis.api.diagnostics.KtNonBoundToPsiErrorDiagnostic
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.getCandidateSymbols
import org.jetbrains.kotlin.analysis.api.fir.isInvokeFunction
import org.jetbrains.kotlin.analysis.api.fir.scopes.getConstructors
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirArrayOfSymbolProvider.arrayOf
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirArrayOfSymbolProvider.arrayOfSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirArrayOfSymbolProvider.arrayTypeToArrayOfCall
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirFunctionSymbol
import org.jetbrains.kotlin.analysis.api.getModule
import org.jetbrains.kotlin.analysis.api.impl.base.components.AbstractKtCallResolver
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.signatures.KtCallableSignature
import org.jetbrains.kotlin.analysis.api.signatures.KtFunctionLikeSignature
import org.jetbrains.kotlin.analysis.api.signatures.KtVariableLikeSignature
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbolOfTypeSafe
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolver.AllCandidatesResolver
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.analysis.utils.errors.withPsiEntry
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnosticHolder
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCall
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.calls.AbstractCandidate
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.resolve.createConeDiagnosticForCandidateWithError
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
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.fir.utils.exceptions.withFirSymbolEntry
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.KtPsiUtil.deparenthesize
import org.jetbrains.kotlin.psi.psiUtil.getPossiblyQualifiedCallExpression
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.toKtPsiSourceElement
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.util.OperatorNameConventions.EQUALS
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.kotlin.utils.exceptions.checkWithAttachment
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.rethrowExceptionWithDetails
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

internal class KtFirCallResolver(
    override val analysisSession: KtFirAnalysisSession,
    override val token: KtLifetimeToken,
) : AbstractKtCallResolver(), KtFirAnalysisSessionComponent {
    private val equalsSymbolInAny: FirNamedFunctionSymbol? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val session = analysisSession.useSiteSession
        val anyFirClass = session.builtinTypes.anyType.toRegularClassSymbol(session) ?: return@lazy null
        val scope = session.declaredMemberScope(
            anyFirClass,
            memberRequiredPhase = FirResolvePhase.STATUS,
        )

        var result: FirNamedFunctionSymbol? = null
        scope.processFunctionsByName(EQUALS) {
            result = it
        }
        result
    }

    override fun resolveCall(psi: KtElement): KtCallInfo? {
        return wrapError(psi) {
            val ktCallInfos = getCallInfo(
                psi,
                getErrorCallInfo = { psiToResolve ->
                    listOf(KtErrorCallInfo(emptyList(), createKtDiagnostic(psiToResolve), token))
                },
                getCallInfo = { psiToResolve, resolveCalleeExpressionOfFunctionCall, resolveFragmentOfCall ->
                    listOfNotNull(
                        toKtCallInfo(
                            psiToResolve,
                            resolveCalleeExpressionOfFunctionCall,
                            resolveFragmentOfCall
                        )
                    )
                }
            )
            check(ktCallInfos.size <= 1) { "Should only return 1 KtCallInfo" }
            ktCallInfos.singleOrNull()
        }
    }

    private inline fun <T> getCallInfo(
        psi: KtElement,
        getErrorCallInfo: FirDiagnosticHolder.(psiToResolve: KtElement) -> List<T>,
        getCallInfo: FirElement.(
            psiToResolve: KtElement,
            resolveCalleeExpressionOfFunctionCall: Boolean,
            resolveFragmentOfCall: Boolean
        ) -> List<T>
    ): List<T> {
        if (!canBeResolvedAsCall(psi)) return emptyList()

        val containingCallExpressionForCalleeExpression = psi.getContainingCallExpressionForCalleeExpression()
        val containingBinaryExpressionForLhs = psi.getContainingBinaryExpressionForIncompleteLhs()
        val containingUnaryExpressionForIncOrDec = psi.getContainingUnaryIncOrDecExpression()
        val psiToResolve = containingCallExpressionForCalleeExpression
            ?: containingBinaryExpressionForLhs
            ?: containingUnaryExpressionForIncOrDec
            ?: psi.getContainingDotQualifiedExpressionForSelectorExpression()
            ?: psi
        val fir = psiToResolve.getOrBuildFir(analysisSession.firResolveSession) ?: return emptyList()
        if (fir is FirDiagnosticHolder) {
            return fir.getErrorCallInfo(psiToResolve)
        }
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

        createGenericTypeQualifierCallIfApplicable(this, psi)?.let { return it }

        if (this is FirResolvedQualifier) {
            val callExpression = (psi as? KtExpression)?.getPossiblyQualifiedCallExpression()
            if (callExpression != null) {
                val constructors = findQualifierConstructors()
                val calls = toKtCalls(constructors)
                return when (calls.size) {
                    0 -> KtErrorCallInfo(listOf(KtQualifierCall(token, callExpression)), inapplicableCandidateDiagnostic(), token)
                    else -> KtErrorCallInfo(calls, inapplicableCandidateDiagnostic(), token)
                }
            }
        }

        if (this is FirImplicitInvokeCall) {

            // If we have a PSI expression like `Foo.Bar.Baz()` and try to resolve `Bar` part,
            // and the only FIR that we have for that PSI is an implicit invoke call, that means that
            // `Foo.Bar` is definitely not a property access - otherwise it would have had its own FIR.
            // So, it does not make sense to try to resolve such parts of qualifiers as KtCall
            if ((psi as? KtExpression)?.getPossiblyQualifiedCallExpression() == null) {
                return null
            }

            if (resolveCalleeExpressionOfFunctionCall) {
                // For implicit invoke, we resolve the calleeExpression of the CallExpression to the call that creates the receiver of this
                // implicit invoke call. For example,
                // ```
                // fun test(f: () -> Unit) {
                //   f() // calleeExpression `f` resolves to the local variable access, while `f()` resolves to the implicit `invoke` call.
                //       // This way `f` is also the explicit receiver of this implicit `invoke` call
                // }
                // ```
                val psiToResolve = (psi as? KtCallExpression)?.calleeExpression ?: psi
                return explicitReceiver?.toKtCallInfo(
                    psiToResolve,
                    resolveCalleeExpressionOfFunctionCall = false,
                    resolveFragmentOfCall = resolveFragmentOfCall
                )
            }
        }

        fun <T> transformErrorReference(
            call: FirElement,
            calleeReference: T,
        ): KtCallInfo where T : FirNamedReference, T : FirDiagnosticHolder {
            val diagnostic = calleeReference.diagnostic
            val ktDiagnostic = calleeReference.createKtDiagnostic(psi)

            if (diagnostic is ConeHiddenCandidateError)
                return KtErrorCallInfo(emptyList(), ktDiagnostic, token)

            val candidateCalls = mutableListOf<KtCall>()
            if (diagnostic is ConeDiagnosticWithCandidates) {
                diagnostic.candidates.mapNotNullTo(candidateCalls) {
                    createKtCall(psi, call, calleeReference, it, resolveFragmentOfCall)
                }
            } else {
                candidateCalls.addIfNotNull(createKtCall(psi, call, calleeReference, null, resolveFragmentOfCall))
            }
            return KtErrorCallInfo(candidateCalls, ktDiagnostic, token)
        }

        return when (this) {
            is FirResolvable, is FirVariableAssignment -> {
                when (val calleeReference = toReference(analysisSession.useSiteSession)) {
                    is FirResolvedErrorReference -> transformErrorReference(this, calleeReference)
                    is FirResolvedNamedReference -> when (calleeReference.resolvedSymbol) {
                        // `calleeReference.resolvedSymbol` isn't guaranteed to be callable. For example, function type parameters used in
                        // expression positions (e.g. `T` in `println(T)`) are parsed as `KtSimpleNameExpression` and built into
                        // `FirPropertyAccessExpression` (which is `FirResolvable`).
                        is FirCallableSymbol<*> -> {
                            val call = createKtCall(psi, this, calleeReference, null, resolveFragmentOfCall)
                                ?: errorWithFirSpecificEntries(
                                    "expect `createKtCall` to succeed for resolvable case with callable symbol",
                                    fir = this,
                                    psi = psi
                                )
                            KtSuccessCallInfo(call)
                        }
                        else -> null
                    }
                    is FirErrorNamedReference -> transformErrorReference(this, calleeReference)
                    // Unresolved delegated constructor call is untransformed and end up as an `FirSuperReference`
                    is FirSuperReference -> {
                        val delegatedConstructorCall = this as? FirDelegatedConstructorCall ?: return null
                        val errorTypeRef = delegatedConstructorCall.constructedTypeRef as? FirErrorTypeRef ?: return null
                        val psiSource = psi.toKtPsiSourceElement()
                        val ktDiagnostic = errorTypeRef.diagnostic.asKtDiagnostic(source ?: psiSource, psiSource) ?: return null
                        KtErrorCallInfo(emptyList(), ktDiagnostic, token)
                    }
                    else -> null
                }
            }
            is FirArrayLiteral -> toKtCallInfo()
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
            is FirSmartCastExpression -> originalExpression.toKtCallInfo(
                psi, resolveCalleeExpressionOfFunctionCall, resolveFragmentOfCall
            )
            else -> null
        }
    }

    private fun inapplicableCandidateDiagnostic() = KtNonBoundToPsiErrorDiagnostic(null, "Inapplicable candidate", token)

    /**
     * Resolves call expressions like `Foo<Bar>` or `test.Foo<Bar>` in calls like `Foo<Bar>::foo`, `test.Foo<Bar>::foo` and class literals like `Foo<Bar>`::class.java.
     *
     * We have a separate [KtGenericTypeQualifier] type of [KtCall].
     */
    private fun createGenericTypeQualifierCallIfApplicable(firElement: FirElement, psiElement: KtElement): KtCallInfo? {
        if (psiElement !is KtExpression) return null
        if (firElement !is FirResolvedQualifier) return null

        val call = psiElement.getPossiblyQualifiedCallExpression() ?: return null
        if (call.typeArgumentList == null || call.valueArgumentList != null) return null

        val parentReferenceExpression = psiElement.parent as? KtDoubleColonExpression ?: return null
        if (parentReferenceExpression.lhs != psiElement) return null

        return KtSuccessCallInfo(KtGenericTypeQualifier(token, psiElement))
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
        if (binaryExpression.operationToken !in KtTokens.ALL_ASSIGNMENTS) return null
        val leftOfBinary = deparenthesize(binaryExpression.left)
        if (leftOfBinary != lhs && !(leftOfBinary is KtDotQualifiedExpression && leftOfBinary.selectorExpression == lhs)) return null
        val firBinaryExpression = binaryExpression.getOrBuildFir(analysisSession.firResolveSession)
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
        return createKtCall(psi, fir, fir.calleeReference, candidate, resolveFragmentOfCall)
    }

    private fun createKtCall(
        psi: KtElement,
        fir: FirElement,
        calleeReference: FirReference,
        candidate: AbstractCandidate?,
        resolveFragmentOfCall: Boolean
    ): KtCall? {
        val targetSymbol = candidate?.symbol
            ?: calleeReference.toResolvedBaseSymbol()
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
        val substitutor = when (fir) {
            is FirQualifiedAccessExpression -> fir.createSubstitutorFromTypeArguments(targetSymbol)
            is FirVariableAssignment -> fir.unwrapLValue()?.createSubstitutorFromTypeArguments(targetSymbol)
            else -> null
        } ?: KtSubstitutor.Empty(token)

        fun createKtPartiallyAppliedSymbolForImplicitInvoke(
            dispatchReceiver: FirExpression?,
            extensionReceiver: FirExpression?,
            explicitReceiverKind: ExplicitReceiverKind
        ): KtPartiallyAppliedSymbol<KtCallableSymbol, KtCallableSignature<KtCallableSymbol>> {
            isImplicitInvoke = true

            // For implicit invoke, the explicit receiver is always set in FIR and this receiver is the variable or property that has
            // the `invoke` member function. In this case, we use the `calleeExpression` in the `KtCallExpression` as the PSI
            // representation of this receiver. Caller can then use this PSI for further call resolution, which is implemented by the
            // parameter `resolveCalleeExpressionOfFunctionCall` in `toKtCallInfo`.
            var explicitReceiverPsi = when (psi) {
                is KtQualifiedExpression -> {
                    psi.selectorExpression
                        ?: errorWithAttachment("missing selectorExpression in PSI ${psi::class} for FirImplicitInvokeCall") {
                            withPsiEntry("psi", psi, analysisSession::getModule)
                        }
                }
                is KtExpression -> psi
                else -> errorWithAttachment("unexpected PSI ${psi::class} for FirImplicitInvokeCall") {
                    withPsiEntry("psi", psi, analysisSession::getModule)
                }
            }

            if (explicitReceiverPsi is KtCallExpression) {
                explicitReceiverPsi = explicitReceiverPsi.calleeExpression
                    ?: errorWithAttachment("missing calleeExpression in PSI ${psi::class} for FirImplicitInvokeCall") {
                        withPsiEntry("psi", psi, analysisSession::getModule)
                    }
            }

            // Specially handle @ExtensionFunctionType
            if (dispatchReceiver?.resolvedType?.isExtensionFunctionType == true) {
                firstArgIsExtensionReceiver = true
            }

            val dispatchReceiverValue: KtReceiverValue?
            val extensionReceiverValue: KtReceiverValue?
            when (explicitReceiverKind) {
                ExplicitReceiverKind.DISPATCH_RECEIVER -> {
                    checkWithAttachment(
                        dispatchReceiver != null,
                        { "Dispatch receiver must be not null if explicitReceiverKind is DISPATCH_RECEIVER" }
                    ) {
                        withPsiEntry("explicitReceiverPsi", explicitReceiverPsi)
                        extensionReceiver?.let { withFirEntry("extensionReceiver", it) }
                        withFirSymbolEntry("target", targetSymbol)
                    }
                    dispatchReceiverValue =
                        KtExplicitReceiverValue(explicitReceiverPsi, dispatchReceiver.resolvedType.asKtType(), false, token)
                    extensionReceiverValue = if (firstArgIsExtensionReceiver) {
                        when (fir) {
                            is FirFunctionCall -> fir.arguments.firstOrNull()?.toKtReceiverValue()
                            is FirPropertyAccessExpression -> fir.explicitReceiver?.toKtReceiverValue()
                            else -> null
                        }
                    } else {
                        extensionReceiver?.toKtReceiverValue()
                    }
                }

                ExplicitReceiverKind.EXTENSION_RECEIVER -> {
                    checkWithAttachment(
                        extensionReceiver != null,
                        { "Extension receiver must be not null if explicitReceiverKind is EXTENSION_RECEIVER" }
                    ) {
                        withPsiEntry("explicitReceiverPsi", explicitReceiverPsi)
                        dispatchReceiver?.let { withFirEntry("dispatchReceiver", it) }
                        withFirSymbolEntry("target", targetSymbol)
                    }

                    dispatchReceiverValue = dispatchReceiver?.toKtReceiverValue()
                    extensionReceiverValue =
                        KtExplicitReceiverValue(explicitReceiverPsi, extensionReceiver.resolvedType.asKtType(), false, token)
                }

                else -> {
                    errorWithAttachment("Implicit invoke call can not have no explicit receiver") {
                        withPsiEntry("explicitReceiverPsi", explicitReceiverPsi)
                        withFirSymbolEntry("targetSymbol", targetSymbol)
                        dispatchReceiver?.let { withFirEntry("dispatchReceiver", it) }
                        extensionReceiver?.let { withFirEntry("extensionReceiver", it) }
                    }
                }
            }
            return KtPartiallyAppliedSymbol(
                with(analysisSession) { unsubstitutedKtSignature.substitute(substitutor) },
                dispatchReceiverValue,
                extensionReceiverValue,
            )
        }

        val partiallyAppliedSymbol = if (candidate != null) {
            if (fir is FirImplicitInvokeCall ||
                (calleeReference.calleeOrCandidateName != OperatorNameConventions.INVOKE && targetSymbol.isInvokeFunction())
            ) {
                // Implicit invoke (e.g., `x()`) will have a different callee symbol (e.g., `x`) than the candidate (e.g., `invoke`).
                createKtPartiallyAppliedSymbolForImplicitInvoke(
                    candidate.dispatchReceiver,
                    candidate.chosenExtensionReceiver,
                    candidate.explicitReceiverKind
                )
            } else {
                KtPartiallyAppliedSymbol(
                    with(analysisSession) { unsubstitutedKtSignature.substitute(substitutor) },
                    candidate.dispatchReceiver?.toKtReceiverValue(),
                    candidate.chosenExtensionReceiver?.toKtReceiverValue(),
                )
            }
        } else if (fir is FirImplicitInvokeCall) {
            val explicitReceiverKind = if (fir.explicitReceiver == fir.dispatchReceiver) {
                ExplicitReceiverKind.DISPATCH_RECEIVER
            } else {
                ExplicitReceiverKind.EXTENSION_RECEIVER
            }
            createKtPartiallyAppliedSymbolForImplicitInvoke(fir.dispatchReceiver, fir.extensionReceiver, explicitReceiverKind)
        } else if (fir is FirQualifiedAccessExpression) {
            KtPartiallyAppliedSymbol(
                with(analysisSession) { unsubstitutedKtSignature.substitute(substitutor) },
                fir.dispatchReceiver?.toKtReceiverValue(),
                fir.extensionReceiver?.toKtReceiverValue()
            )
        } else if (fir is FirVariableAssignment) {
            KtPartiallyAppliedSymbol(
                with(analysisSession) { unsubstitutedKtSignature.substitute(substitutor) },
                fir.dispatchReceiver?.toKtReceiverValue(),
                fir.extensionReceiver?.toKtReceiverValue()
            )
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
                    fir.unwrapLValue()?.toTypeArgumentsMapping(partiallyAppliedSymbol) ?: emptyMap(),
                    KtSimpleVariableAccess.Write(rhs)
                )
            }
            is FirPropertyAccessExpression -> {
                when (unsubstitutedKtSignature.symbol) {
                    is KtVariableLikeSymbol -> {
                        @Suppress("UNCHECKED_CAST") // safe because of the above check on targetKtSymbol
                        KtSimpleVariableAccessCall(
                            partiallyAppliedSymbol as KtPartiallyAppliedVariableSymbol<KtVariableLikeSymbol>,
                            fir.toTypeArgumentsMapping(partiallyAppliedSymbol),
                            KtSimpleVariableAccess.Read
                        )
                    }
                    // if errorsness call without ()
                    is KtFunctionLikeSymbol -> {
                        @Suppress("UNCHECKED_CAST") // safe because of the above check on targetKtSymbol
                        KtSimpleFunctionCall(
                            partiallyAppliedSymbol as KtPartiallyAppliedFunctionSymbol<KtFunctionLikeSymbol>,
                            LinkedHashMap(),
                            fir.toTypeArgumentsMapping(partiallyAppliedSymbol),
                            isImplicitInvoke,
                        )
                    }
                }
            }
            is FirFunctionCall -> {
                if (unsubstitutedKtSignature.symbol !is KtFunctionLikeSymbol) return null
                val argumentMapping = if (candidate is Candidate) {
                    candidate.argumentMapping
                } else {
                    fir.resolvedArgumentMapping
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
                    @Suppress("USELESS_CAST") // K2 warning suppression, TODO: KT-62472
                    argumentMappingWithoutExtensionReceiver
                        ?.createArgumentMapping(partiallyAppliedSymbol.signature as KtFunctionLikeSignature<*>)
                        ?: LinkedHashMap(),
                    fir.toTypeArgumentsMapping(partiallyAppliedSymbol),
                    isImplicitInvoke
                )
            }
            is FirSmartCastExpression -> (fir.originalExpression as? FirResolvable)?.let {
                createKtCall(
                    psi,
                    it,
                    candidate,
                    resolveFragmentOfCall
                )
            }
            else -> null
        }
    }

    private fun handleCompoundAccessCall(psi: KtElement, fir: FirElement, resolveFragmentOfCall: Boolean): KtCall? {
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
                    KtSimpleFunctionCall(
                        getPartiallyAppliedSymbol,
                        getAccessArgumentMapping,
                        fir.toTypeArgumentsMapping(getPartiallyAppliedSymbol),
                        false
                    )
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
                    KtSimpleVariableAccessCall(
                        variablePartiallyAppliedSymbol,
                        fir.unwrapLValue()?.toTypeArgumentsMapping(variablePartiallyAppliedSymbol) ?: emptyMap(),
                        KtSimpleVariableAccess.Read
                    )
                } else {
                    KtCompoundVariableAccessCall(
                        variablePartiallyAppliedSymbol,
                        fir.unwrapLValue()?.toTypeArgumentsMapping(variablePartiallyAppliedSymbol) ?: emptyMap(),
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
                    KtSimpleFunctionCall(
                        getPartiallyAppliedSymbol,
                        getAccessArgumentMapping,
                        fir.toTypeArgumentsMapping(getPartiallyAppliedSymbol),
                        false
                    )
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
                    KtSimpleVariableAccessCall(
                        variablePartiallyAppliedSymbol,
                        fir.unwrapLValue()?.toTypeArgumentsMapping(variablePartiallyAppliedSymbol) ?: emptyMap(),
                        KtSimpleVariableAccess.Read
                    )
                } else {
                    KtCompoundVariableAccessCall(
                        variablePartiallyAppliedSymbol,
                        fir.unwrapLValue()?.toTypeArgumentsMapping(variablePartiallyAppliedSymbol) ?: emptyMap(),
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

    private fun getOperationPartiallyAppliedSymbolsForIncOrDecOperation(
        fir: FirFunctionCall,
        arrayAccessExpression: KtArrayAccessExpression,
        incDecPrecedence: KtCompoundAccess.IncOrDecOperation.Precedence
    ): CompoundArrayAccessPartiallyAppliedSymbols? {
        val lastArg = fir.arguments.lastOrNull() ?: return null
        val setPartiallyAppliedSymbol = fir.toPartiallyAppliedSymbol(arrayAccessExpression.arrayExpression) ?: return null
        return when (incDecPrecedence) {
            KtCompoundAccess.IncOrDecOperation.Precedence.PREFIX -> {
                // For prefix case, the last argument is a call to get(...).inc().
                val operationCall = lastArg as? FirFunctionCall ?: return null
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
        return variableReference.toReference(firResolveSession.useSiteFirSession)
            ?.toResolvedVariableSymbol()
            ?.fir
            ?.initializer as? FirFunctionCall
    }

    private fun getOperationPartiallyAppliedSymbolsForCompoundVariableAccess(
        fir: FirVariableAssignment,
        leftOperandPsi: KtExpression
    ): KtPartiallyAppliedFunctionSymbol<KtFunctionSymbol>? {
        // The new value is a call to the appropriate operator function.
        val operationCall = fir.rValue as? FirFunctionCall ?: getInitializerOfReferencedLocalVariable(fir.rValue) ?: return null
        return operationCall.toPartiallyAppliedSymbol(leftOperandPsi)
    }

    private fun FirVariableAssignment.toPartiallyAppliedSymbol(): KtPartiallyAppliedVariableSymbol<KtVariableLikeSymbol>? {
        val variableRef = calleeReference as? FirResolvedNamedReference ?: return null
        val variableSymbol = variableRef.resolvedSymbol as? FirVariableSymbol<*> ?: return null
        val substitutor = unwrapLValue()?.createConeSubstitutorFromTypeArguments() ?: return null
        val ktSignature = variableSymbol.toKtSignature()
        return KtPartiallyAppliedSymbol(
            with(analysisSession) { ktSignature.substitute(substitutor.toKtSubstitutor()) },
            dispatchReceiver?.toKtReceiverValue(),
            extensionReceiver?.toKtReceiverValue(),
        )
    }

    private fun FirFunctionCall.toPartiallyAppliedSymbol(
        explicitReceiverPsiSupplement: KtExpression? = null
    ): KtPartiallyAppliedFunctionSymbol<KtFunctionSymbol>? {
        val operationSymbol =
            (calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirNamedFunctionSymbol ?: return null
        val substitutor = createConeSubstitutorFromTypeArguments() ?: return null
        val explicitReceiver = this.explicitReceiver
        val dispatchReceiver = this.dispatchReceiver
        val extensionReceiver = this.extensionReceiver

        checkWithAttachment(
            (explicitReceiver != null) == (explicitReceiverPsiSupplement != null),
            { "FIR and PSI for explicit receiver are inconsistent (one of them is null)" }
        ) {
            withPsiEntry("explicitReceiverPsi", explicitReceiverPsiSupplement)
            if (explicitReceiver != null) {
                withFirEntry("explicitReceiverFir", explicitReceiver)
            } else {
                withEntry("explicitReceiverFir", "null")
            }
        }

        val dispatchReceiverValue = if (explicitReceiverPsiSupplement != null && explicitReceiver == dispatchReceiver) {
            explicitReceiverPsiSupplement.toExplicitReceiverValue(dispatchReceiver!!.resolvedType.asKtType())
        } else {
            dispatchReceiver?.toKtReceiverValue()
        }
        val extensionReceiverValue = if (explicitReceiverPsiSupplement != null && explicitReceiver == extensionReceiver) {
            explicitReceiverPsiSupplement.toExplicitReceiverValue(extensionReceiver!!.resolvedType.asKtType())
        } else {
            extensionReceiver?.toKtReceiverValue()
        }
        val ktSignature = operationSymbol.toKtSignature()
        return KtPartiallyAppliedSymbol(
            with(analysisSession) { ktSignature.substitute(substitutor.toKtSubstitutor()) },
            dispatchReceiverValue,
            extensionReceiverValue,
        )
    }

    private fun FirExpression.toKtReceiverValue(): KtReceiverValue? {
        return when {
            this is FirSmartCastExpression -> {
                val result = originalExpression.toKtReceiverValue()
                if (result != null && isStable) {
                    KtSmartCastedReceiverValue(result, smartcastType.coneType.asKtType())
                } else {
                    result
                }
            }
            this is FirThisReceiverExpression && this.isImplicit -> {
                val implicitPartiallyAppliedSymbol = when (val partiallyAppliedSymbol = calleeReference.boundSymbol) {
                    is FirClassSymbol<*> -> partiallyAppliedSymbol.toKtSymbol()
                    is FirCallableSymbol<*> -> firSymbolBuilder.callableBuilder.buildExtensionReceiverSymbol(partiallyAppliedSymbol)
                        ?: return null
                    else -> return null
                }
                KtImplicitReceiverValue(implicitPartiallyAppliedSymbol, resolvedType.asKtType())
            }
            else -> {
                val psi = psi
                if (psi !is KtExpression) return null
                psi.toExplicitReceiverValue(resolvedType.asKtType())
            }
        }
    }

    private fun FirCallableSymbol<*>.toKtSignature(): KtCallableSignature<KtCallableSymbol> =
        firSymbolBuilder.callableBuilder.buildCallableSignature(this)

    private fun FirClassLikeSymbol<*>.toKtSymbol(): KtClassLikeSymbol = firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(this)

    private fun FirNamedFunctionSymbol.toKtSignature(): KtFunctionLikeSignature<KtFunctionSymbol> =
        firSymbolBuilder.functionLikeBuilder.buildFunctionSignature(this)

    private fun FirVariableSymbol<*>.toKtSignature(): KtVariableLikeSignature<KtVariableLikeSymbol> =
        firSymbolBuilder.variableLikeBuilder.buildVariableLikeSignature(this)

    private fun FirQualifiedAccessExpression.toTypeArgumentsMapping(
        partiallyAppliedSymbol: KtPartiallyAppliedSymbol<*, *>
    ): Map<KtTypeParameterSymbol, KtType> {
        return toTypeArgumentsMapping(typeArguments, partiallyAppliedSymbol)
    }

    private fun FirResolvedQualifier.toTypeArgumentsMapping(
        partiallyAppliedSymbol: KtPartiallyAppliedSymbol<*, *>
    ): Map<KtTypeParameterSymbol, KtType> {
        return toTypeArgumentsMapping(typeArguments, partiallyAppliedSymbol)
    }

    /**
     * Maps [typeArguments] to the type parameters of [partiallyAppliedSymbol].
     *
     * If too many type arguments are provided, a mapping is still created. Extra type arguments are simply ignored. If this wasn't the
     * case, the resulting [KtCall] would contain no type arguments at all, which can cause problems later. If too few type arguments are
     * provided, an empty map is returned defensively so that [toTypeArgumentsMapping] doesn't conjure any error types. If you want to map
     * too few type arguments meaningfully, please provide filler types explicitly.
     */
    private fun toTypeArgumentsMapping(
        typeArguments: List<FirTypeProjection>,
        partiallyAppliedSymbol: KtPartiallyAppliedSymbol<*, *>
    ): Map<KtTypeParameterSymbol, KtType> {
        val typeParameters = partiallyAppliedSymbol.symbol.typeParameters
        if (typeParameters.isEmpty()) return emptyMap()
        if (typeArguments.size < typeParameters.size) return emptyMap()

        val result = mutableMapOf<KtTypeParameterSymbol, KtType>()

        for ((index, typeParameter) in typeParameters.withIndex()) {
            // After resolution all type arguments should be usual types (not FirPlaceholderProjection)
            val typeArgument = typeArguments[index]
            if (typeArgument !is FirTypeProjectionWithVariance || typeArgument.variance != Variance.INVARIANT) return emptyMap()
            result[typeParameter] = typeArgument.typeRef.coneType.asKtType()
        }

        return result
    }

    private fun FirArrayLiteral.toTypeArgumentsMapping(
        partiallyAppliedSymbol: KtPartiallyAppliedSymbol<*, *>
    ): Map<KtTypeParameterSymbol, KtType> {
        val elementType = resolvedType.arrayElementType()?.asKtType() ?: return emptyMap()
        val typeParameter = partiallyAppliedSymbol.symbol.typeParameters.singleOrNull() ?: return emptyMap()
        return mapOf(typeParameter to elementType)
    }

    override fun collectCallCandidates(psi: KtElement): List<KtCallCandidateInfo> = wrapError(psi) {
        getCallInfo(
            psi,
            getErrorCallInfo = { emptyList() },
            getCallInfo = { psiToResolve, resolveCalleeExpressionOfFunctionCall, resolveFragmentOfCall ->
                collectCallCandidates(
                    psiToResolve,
                    resolveCalleeExpressionOfFunctionCall,
                    resolveFragmentOfCall
                )
            }
        )
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
            is FirArrayLiteral, is FirEqualityOperatorCall -> {
                toKtCallInfo(psi, resolveCalleeExpressionOfFunctionCall, resolveFragmentOfCall).toKtCallCandidateInfos()
            }
            is FirComparisonExpression -> compareToCall.toKtCallInfo(
                psi,
                resolveCalleeExpressionOfFunctionCall,
                resolveFragmentOfCall
            ).toKtCallCandidateInfos()
            is FirResolvedQualifier -> toKtCallCandidateInfos()
            is FirDelegatedConstructorCall -> collectCallCandidatesForDelegatedConstructorCall(psi, resolveFragmentOfCall)
            else -> toKtCallInfo(psi, resolveCalleeExpressionOfFunctionCall, resolveFragmentOfCall).toKtCallCandidateInfos()
        }
    }

    private fun FirResolvedQualifier.toKtCallCandidateInfos(): List<KtCallCandidateInfo> {
        return toKtCalls(findQualifierConstructors()).map {
            KtInapplicableCallCandidateInfo(
                it,
                isInBestCandidates = false,
                _diagnostic = inapplicableCandidateDiagnostic()
            )
        }
    }

    private fun FirResolvedQualifier.findQualifierConstructors(): List<KtConstructorSymbol> {
        val classSymbol = this.symbol?.fullyExpandedClass(analysisSession.useSiteSession) ?: return emptyList()
        return classSymbol.unsubstitutedScope(
            analysisSession.useSiteSession,
            analysisSession.getScopeSessionFor(analysisSession.useSiteSession),
            withForcedTypeCalculator = true,
            memberRequiredPhase = null,
        )
            .getConstructors(analysisSession.firSymbolBuilder)
            .toList()
    }

    private fun FirResolvedQualifier.toKtCalls(constructors: List<KtConstructorSymbol>): List<KtCall> {
        analysisSession.apply {
            return constructors.map { constructor ->
                val partiallyAppliedSymbol = KtPartiallyAppliedFunctionSymbol(constructor.asSignature(), null, null)
                KtSimpleFunctionCall(partiallyAppliedSymbol, LinkedHashMap(), toTypeArgumentsMapping(partiallyAppliedSymbol), false)
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
        val candidates = AllCandidatesResolver(analysisSession.useSiteSession).getAllCandidates(
            analysisSession.firResolveSession,
            originalFunctionCall,
            calleeName,
            psi,
            ResolutionMode.ContextIndependent,
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

    private fun FirDelegatedConstructorCall.collectCallCandidatesForDelegatedConstructorCall(
        psi: KtElement,
        resolveFragmentOfCall: Boolean
    ): List<KtCallCandidateInfo> {
        fun findDerivedClass(psi: KtElement): KtClassOrObject? {
            val parent = psi.parent
            return when (psi) {
                is KtConstructorDelegationCall -> ((parent as? KtSecondaryConstructor)?.parent as? KtClassBody)?.parent as? KtClassOrObject
                is KtSuperTypeCallEntry -> (parent as? KtSuperTypeList)?.parent as? KtClassOrObject
                is KtConstructorCalleeExpression -> (parent as? KtElement)?.let(::findDerivedClass)
                else -> null
            }
        }

        val derivedClass = findDerivedClass(psi)?.resolveToFirSymbolOfTypeSafe<FirClassSymbol<*>>(firResolveSession) ?: return emptyList()

        val candidates = AllCandidatesResolver(analysisSession.useSiteSession)
            .getAllCandidatesForDelegatedConstructor(analysisSession.firResolveSession, this, derivedClass.toLookupTag(), psi)

        return candidates.mapNotNull {
            convertToKtCallCandidateInfo(
                this,
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
        resolvable: FirResolvable,
        element: KtElement,
        candidate: Candidate,
        isInBestCandidates: Boolean,
        resolveFragmentOfCall: Boolean
    ): KtCallCandidateInfo? {
        val call = createKtCall(element, resolvable, candidate, resolveFragmentOfCall)
            ?: error("expect `createKtCall` to succeed for candidate")
        if (candidate.isSuccessful) {
            return KtApplicableCallCandidateInfo(call, isInBestCandidates)
        }

        val diagnostic = createConeDiagnosticForCandidateWithError(candidate.currentApplicability, candidate)
        if (diagnostic is ConeHiddenCandidateError) return null
        val ktDiagnostic =
            resolvable.source?.let { diagnostic.asKtDiagnostic(it, element.toKtPsiSourceElement()) }
                ?: KtNonBoundToPsiErrorDiagnostic(factoryName = null, diagnostic.reason, token)
        return KtInapplicableCallCandidateInfo(call, isInBestCandidates, ktDiagnostic)
    }

    private val FirResolvable.calleeOrCandidateName: Name?
        get() = this.calleeReference.calleeOrCandidateName

    private val FirReference.calleeOrCandidateName: Name?
        get() {
            if (this !is FirNamedReference) return null

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
            return when (val psi = psi) {
                is KtNameReferenceExpression -> psi.getReferencedNameAsName()
                else -> {
                    // This could be KtArrayAccessExpression or KtOperationReferenceExpression.
                    // Note: All candidate symbols should have the same name. We go by the symbol because `calleeReference.name` will include
                    // the applicability if not successful.
                    getCandidateSymbols().firstOrNull()?.safeAs<FirCallableSymbol<*>>()?.name
                }
            }
        }

    private fun FirArrayLiteral.toKtCallInfo(): KtCallInfo? {
        val arrayOfSymbol = with(analysisSession) {

            val type = resolvedType as? ConeClassLikeType
                ?: return run {
                    val defaultArrayOfSymbol = arrayOfSymbol(arrayOf) ?: return null
                    val substitutor = createSubstitutorFromTypeArguments(defaultArrayOfSymbol)
                    val partiallyAppliedSymbol = KtPartiallyAppliedSymbol(
                        with(analysisSession) { defaultArrayOfSymbol.substitute(substitutor) },
                        null,
                        null,
                    )
                    KtErrorCallInfo(
                        listOf(
                            KtSimpleFunctionCall(
                                partiallyAppliedSymbol,
                                createArgumentMapping(defaultArrayOfSymbol, substitutor),
                                this@toKtCallInfo.toTypeArgumentsMapping(partiallyAppliedSymbol),
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
        val partiallyAppliedSymbol = KtPartiallyAppliedSymbol(
            with(analysisSession) { arrayOfSymbol.substitute(substitutor) },
            null,
            null,
        )
        return KtSuccessCallInfo(
            KtSimpleFunctionCall(
                partiallyAppliedSymbol,
                createArgumentMapping(arrayOfSymbol, substitutor),
                this@toKtCallInfo.toTypeArgumentsMapping(partiallyAppliedSymbol),
                false
            )
        )
    }

    private fun FirArrayLiteral.createSubstitutorFromTypeArguments(arrayOfSymbol: KtFirFunctionSymbol): KtSubstitutor {
        val firSymbol = arrayOfSymbol.firSymbol
        // No type parameter means this is an arrayOf call of primitives, in which case there is no type arguments
        val typeParameter = firSymbol.fir.typeParameters.singleOrNull() ?: return KtSubstitutor.Empty(token)

        val elementType = resolvedType.arrayElementType() ?: return KtSubstitutor.Empty(token)
        val coneSubstitutor = substitutorByMap(mapOf(typeParameter.symbol to elementType), rootModuleSession)
        return firSymbolBuilder.typeBuilder.buildSubstitutor(coneSubstitutor)
    }


    private fun FirEqualityOperatorCall.toKtCallInfo(psi: KtElement): KtCallInfo? {
        val binaryExpression = deparenthesize(psi as? KtExpression) as? KtBinaryExpression ?: return null
        val leftPsi = binaryExpression.left ?: return null
        val rightPsi = binaryExpression.right ?: return null
        return when (operation) {
            FirOperation.EQ, FirOperation.NOT_EQ -> {
                val leftOperand = arguments.firstOrNull() ?: return null
                val session = analysisSession.useSiteSession
                val leftOperandType = leftOperand.resolvedType

                val classSymbol = leftOperandType.fullyExpandedType(session).toSymbol(session) as? FirClassSymbol<*>
                val equalsSymbol = classSymbol?.getEqualsSymbol() ?: equalsSymbolInAny ?: return null
                val ktSignature = equalsSymbol.toKtSignature()
                KtSuccessCallInfo(
                    KtSimpleFunctionCall(
                        KtPartiallyAppliedSymbol(
                            ktSignature,
                            KtExplicitReceiverValue(leftPsi, leftOperandType.asKtType(), false, token),
                            null
                        ),
                        LinkedHashMap<KtExpression, KtVariableLikeSignature<KtValueParameterSymbol>>().apply {
                            put(rightPsi, ktSignature.valueParameters.first())
                        },
                        emptyMap(),
                        false
                    )
                )
            }
            else -> null
        }
    }

    private fun FirClassSymbol<*>.getEqualsSymbol(): FirNamedFunctionSymbol? {
        val scope = unsubstitutedScope(
            analysisSession.useSiteSession,
            analysisSession.getScopeSessionFor(analysisSession.useSiteSession),
            false,
            memberRequiredPhase = FirResolvePhase.STATUS,
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
        return resolvedArgumentMapping?.entries.createArgumentMapping(signatureOfCallee)
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

    private fun FirArrayLiteral.createArgumentMapping(
        arrayOfSymbol: KtFirFunctionSymbol,
        substitutor: KtSubstitutor,
    ): LinkedHashMap<KtExpression, KtVariableLikeSignature<KtValueParameterSymbol>> {
        val ktArgumentMapping = LinkedHashMap<KtExpression, KtVariableLikeSignature<KtValueParameterSymbol>>()
        val parameterSymbol = arrayOfSymbol.valueParameters.single()

        for (firExpression in argumentList.arguments) {
            mapArgumentExpressionToParameter(
                firExpression,
                with(analysisSession) { parameterSymbol.substitute(substitutor) },
                ktArgumentMapping
            )
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
        // For smart-casted expression, refer to the source of the original expression
        // For spread, named, and lambda arguments, the source is the KtValueArgument.
        // For other arguments (including array indices), the source is the KtExpression.
        return when (this) {
            is FirSamConversionExpression ->
                expression.realPsi as? KtExpression
            is FirSmartCastExpression ->
                originalExpression.realPsi as? KtExpression
            is FirNamedArgumentExpression, is FirSpreadArgumentExpression, is FirLambdaArgumentExpression ->
                realPsi.safeAs<KtValueArgument>()?.getArgumentExpression()
            else -> realPsi as? KtExpression
        }
    }

    private inline fun <R> wrapError(element: KtElement, action: () -> R): R {
        return try {
            action()
        } catch (e: Exception) {
            rethrowExceptionWithDetails(
                "Error during resolving call ${element::class}",
                exception = e,
            ) {
                withPsiEntry("psi", element, analysisSession::getModule)
                element.getOrBuildFir(firResolveSession)?.let { withFirEntry("fir", it) }
            }
        }

    }

    private fun FirDiagnosticHolder.createKtDiagnostic(psi: KtElement?): KtDiagnostic {
        return (source?.let { diagnostic.asKtDiagnostic(it, psi?.toKtPsiSourceElement()) }
            ?: KtNonBoundToPsiErrorDiagnostic(factoryName = null, diagnostic.reason, token))
    }
}
