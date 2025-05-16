/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.openapi.diagnostic.logger
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostic
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.getCandidateSymbols
import org.jetbrains.kotlin.analysis.api.fir.isInvokeFunction
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirArrayOfSymbolProvider.arrayOf
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirArrayOfSymbolProvider.arrayOfSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirArrayOfSymbolProvider.arrayTypeToArrayOfCall
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.processEqualsFunctions
import org.jetbrains.kotlin.analysis.api.getModule
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseResolver
import org.jetbrains.kotlin.analysis.api.impl.base.components.withPsiValidityAssertion
import org.jetbrains.kotlin.analysis.api.impl.base.resolution.*
import org.jetbrains.kotlin.analysis.api.impl.base.util.KaNonBoundToPsiErrorDiagnostic
import org.jetbrains.kotlin.analysis.api.resolution.*
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaFunctionSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableSignature
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.utils.errors.withPsiEntry
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbolOfTypeSafe
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolver.AllCandidatesResolver
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.findStringPlusSymbol
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
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
import org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.stages.TypeArgumentMapping
import org.jetbrains.kotlin.fir.resolve.createConeDiagnosticForCandidateWithError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeDiagnosticWithCandidates
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeHiddenCandidateError
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.transformers.unwrapAtoms
import org.jetbrains.kotlin.fir.scopes.getDeclaredConstructors
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.fir.utils.exceptions.withFirSymbolEntry
import org.jetbrains.kotlin.idea.references.KtDefaultAnnotationArgumentReference
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.KtPsiUtil.deparenthesize
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getPossiblyQualifiedCallExpression
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElementSelector
import org.jetbrains.kotlin.resolve.calls.inference.buildCurrentSubstitutor
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.toKtPsiSourceElement
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.util.OperatorNameConventions.EQUALS
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.kotlin.utils.exceptions.*

internal class KaFirResolver(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaBaseResolver<KaFirSession>(), KaFirSessionComponent {
    /**
     * Notes on the implementation:
     *
     * ## Invoke calls
     *
     * For expressions like `Bar()`, if `Bar` implicitly points to the companion object,
     * and `()` is an implicit `invoke` operator call, we cannot use [getOrBuildFir]
     * on `Bar` reference to get the corresponding [FirResolvedQualifier].
     *
     * Instead, we have to find the whole [FirImplicitInvokeCall] for the `Bar()` call,
     * and use explicit receiver from there as the correct qualifier.
     *
     * ## Qualified expressions
     *
     * For expressions like `Foo.Bar`, if `Bar` implicitly points to the companion object,
     * there is actually no [FirResolvedQualifier] for the `Foo` alone -
     * only for the whole `Foo.Bar`.
     *
     * So, when you want to check if `Foo` points to the companion object
     * and call [getOrBuildFir] on it, you receive the qualifier for the
     * whole `Foo.Bar`.
     *
     * Fortunately, you cannot have two references implicitly pointing to the
     * companion object in a single dot-qualified expression - only the
     * last reference in the chain can do that.
     *
     * So, if the PSI element of the [KtReference] and the whole [FirResolvedQualifier]
     * are different, we can certainly say that the [KtReference] does not
     * point to the companion object.
     */
    override fun KtReference.isImplicitReferenceToCompanion(): Boolean = withPsiValidityAssertion(element) {
        if (this !is KtSimpleNameReference) {
            return false
        }

        val implicitInvokeCall = run {
            val parentCallExpression = element.parent as? KtCallExpression
            parentCallExpression?.getOrBuildFir(analysisSession.resolutionFacade) as? FirImplicitInvokeCall
        }

        val wholeQualifier = implicitInvokeCall?.explicitReceiver
            ?: element.getOrBuildFir(analysisSession.resolutionFacade)

        if (wholeQualifier !is FirResolvedQualifier) return false

        val wholeQualifierNameExpression = (wholeQualifier.psi as? KtElement)?.getQualifiedElementSelector() as? KtSimpleNameExpression
        if (wholeQualifierNameExpression != element) return false

        return wholeQualifier.resolvedToCompanionObject
    }

    override fun KtReference.resolveToSymbols(): Collection<KaSymbol> = withPsiValidityAssertion(element) {
        return doResolveToSymbols(this)
    }

    private fun doResolveToSymbols(reference: KtReference): Collection<KaSymbol> {
        if (reference is KtDefaultAnnotationArgumentReference) {
            return resolveDefaultAnnotationArgumentReference(reference)
        }

        checkWithAttachment(
            reference is KaSymbolBasedReference,
            { "${reference::class.simpleName} is not extends ${KaSymbolBasedReference::class.simpleName}" },
        ) {
            withPsiEntry("reference", reference.element)
        }

        with(reference) {
            return analysisSession.resolveToSymbols()
        }
    }

    override fun doResolveCall(psi: KtElement): KaCallInfo? = wrapError(psi) {
        analysisSession.cacheStorage.resolveToCallCache.value.getOrPut(psi) {
            val ktCallInfos = getCallInfo(
                psi,
                getErrorCallInfo = { psiToResolve ->
                    listOf(KaBaseErrorCallInfo(emptyList(), createKtDiagnostic(psiToResolve)))
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

    override fun doCollectCallCandidates(psi: KtElement): List<KaCallCandidateInfo> = wrapError(psi) {
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

    private val equalsSymbolInAny: FirNamedFunctionSymbol? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val session = analysisSession.firSession
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

    private inline fun <T> getCallInfo(
        psi: KtElement,
        getErrorCallInfo: FirDiagnosticHolder.(psiToResolve: KtElement) -> List<T>,
        getCallInfo: FirElement.(
            psiToResolve: KtElement,
            resolveCalleeExpressionOfFunctionCall: Boolean,
            resolveFragmentOfCall: Boolean,
        ) -> List<T>,
    ): List<T> {
        val containingCallExpressionForCalleeExpression = psi.getContainingCallExpressionForCalleeExpression()
        val containingBinaryExpressionForLhs = psi.getContainingBinaryExpressionForIncompleteLhs()
        val containingUnaryExpressionForIncOrDec = psi.getContainingUnaryIncOrDecExpression()
        val psiToResolve = containingCallExpressionForCalleeExpression
            ?: containingBinaryExpressionForLhs
            ?: containingUnaryExpressionForIncOrDec
            ?: psi.getContainingDotQualifiedExpressionForSelectorExpression()
            ?: psi.getConstructorDelegationCallForDelegationReferenceExpression()
            ?: psi
        val fir = psiToResolve.getOrBuildFir(analysisSession.resolutionFacade) ?: return emptyList()
        if (fir is FirDiagnosticHolder) {
            return fir.getErrorCallInfo(psiToResolve)
        }
        return fir.getCallInfo(
            psiToResolve,
            psiToResolve == containingCallExpressionForCalleeExpression,
            psiToResolve == containingBinaryExpressionForLhs || psiToResolve == containingUnaryExpressionForIncOrDec
        )
    }

    private val stringPlusSymbol by lazy(LazyThreadSafetyMode.PUBLICATION) {
        findStringPlusSymbol(analysisSession.firSession)
    }

    private fun FirElement.toKtCallInfo(
        psi: KtElement,
        resolveCalleeExpressionOfFunctionCall: Boolean,
        resolveFragmentOfCall: Boolean,
    ): KaCallInfo? {
        // FIR does not have an intermediate symbol of String.plus() function call in case of folded string literals.
        //
        // Example:
        // Expression `"a" + "b" + "c"` is represented by one FirStringConcatenationCall containing `"a"`, `"b"`, and `"c"` as arguments.
        //
        // We have to patch `FirElement.toKtCallInfo` to return String.plus() call info for contained binary expressions.
        if (this is FirStringConcatenationCall && this.isFoldedStrings && psi is KtBinaryExpression) {
            val leftArg = psi.left ?: return null
            val rightArg = psi.right ?: return null
            val signature = stringPlusSymbol?.toKaSignature() ?: return null
            return KaBaseSuccessCallInfo(
                KaBaseSimpleFunctionCall(
                    KaBasePartiallyAppliedSymbol(
                        signature,
                        KaBaseExplicitReceiverValue(
                            leftArg,
                            analysisSession.builtinTypes.string,
                            false
                        ),
                        null,
                        emptyList(),
                    ),
                    mapOf(rightArg to signature.valueParameters.first()),
                    emptyMap(),
                    false
                )
            )
        }

        if (this is FirResolvedQualifier) {
            val callExpression = (psi as? KtExpression)?.getPossiblyQualifiedCallExpression()
            if (callExpression != null) {
                val constructors = findQualifierConstructors()
                val calls = toKtCalls(constructors)
                return KaBaseErrorCallInfo(calls, inapplicableCandidateDiagnostic())
            }
        }

        if (this is FirImplicitInvokeCall) {

            // If we have a PSI expression like `Foo.Bar.Baz()` and try to resolve `Bar` part,
            // and the only FIR that we have for that PSI is an implicit invoke call, that means that
            // `Foo.Bar` is definitely not a property access - otherwise it would have had its own FIR.
            // So, it does not make sense to try to resolve such parts of qualifiers as KaCall
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
        ): KaCallInfo where T : FirNamedReference, T : FirDiagnosticHolder {
            val diagnostic = calleeReference.diagnostic
            val ktDiagnostic = calleeReference.createKtDiagnostic(psi)

            if (diagnostic is ConeHiddenCandidateError)
                return KaBaseErrorCallInfo(emptyList(), ktDiagnostic)

            val candidateCalls = mutableListOf<KaCall>()
            if (diagnostic is ConeDiagnosticWithCandidates) {
                diagnostic.candidates.mapNotNullTo(candidateCalls) {
                    if (it is Candidate) {
                        createKtCall(psi, call, calleeReference, it, resolveFragmentOfCall)
                    } else {
                        null
                    }
                }
            } else {
                candidateCalls.addIfNotNull(createKtCall(psi, call, calleeReference, null, resolveFragmentOfCall))
            }
            return KaBaseErrorCallInfo(candidateCalls, ktDiagnostic)
        }

        return when (this) {
            is FirResolvable, is FirVariableAssignment -> {
                when (val calleeReference = toReference(analysisSession.firSession)) {
                    is FirResolvedErrorReference -> transformErrorReference(this, calleeReference)
                    is FirResolvedNamedReference -> when (calleeReference.resolvedSymbol) {
                        // `calleeReference.resolvedSymbol` isn't guaranteed to be callable. For example, function type parameters used in
                        // expression positions (e.g. `T` in `println(T)`) are parsed as `KtSimpleNameExpression` and built into
                        // `FirPropertyAccessExpression` (which is `FirResolvable`).
                        is FirCallableSymbol<*> -> {
                            val call = createKtCall(psi, this, calleeReference, null, resolveFragmentOfCall)
                            call?.let(::KaBaseSuccessCallInfo)
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
                        KaBaseErrorCallInfo(emptyList(), ktDiagnostic)
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

    private fun inapplicableCandidateDiagnostic(): KaDiagnostic {
        return KaNonBoundToPsiErrorDiagnostic(factoryName = FirErrors.OTHER_ERROR.name, "Inapplicable candidate", token)
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
        if (leftOfBinary != lhs && !(leftOfBinary is KtQualifiedExpression && leftOfBinary.selectorExpression == lhs)) return null
        val firBinaryExpression = binaryExpression.getOrBuildFir(analysisSession.resolutionFacade)
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
     * When resolving selector expression of a [KtQualifiedExpression], we instead resolve the containing qualified expression. This way
     * the corresponding FIR element is the [FirFunctionCall] or [FirPropertyAccessExpression], etc.
     */
    private fun KtElement.getContainingDotQualifiedExpressionForSelectorExpression(): KtQualifiedExpression? {
        val parent = parent
        if (parent is KtQualifiedExpression && parent.selectorExpression == this) return parent
        return null
    }

    /**
     * When resolving [KtConstructorDelegationReferenceExpression], we instead resolve the containing [KtConstructorDelegationCall].
     * This way the corresponding FIR element is the [FirDelegatedConstructorCall] instead of the reference
     */
    private fun KtElement.getConstructorDelegationCallForDelegationReferenceExpression(): KtConstructorDelegationCall? {
        return takeIf { it is KtConstructorDelegationReferenceExpression }?.parent as? KtConstructorDelegationCall
    }

    private fun createKtCall(
        psi: KtElement,
        fir: FirResolvable,
        candidate: Candidate?,
        resolveFragmentOfCall: Boolean,
    ): KaCall? {
        return createKtCall(psi, fir, fir.calleeReference, candidate, resolveFragmentOfCall)
    }

    private fun Candidate.toFirTypeArgumentsMapping(symbol: FirCallableSymbol<*>): Map<FirTypeParameterSymbol, ConeKotlinType> {
        val typeParameters = symbol.typeParameterSymbols.ifEmpty { return emptyMap() }

        // Maps a type parameter `A` into a type variable `TypeVariable(A)` type
        val candidateSubstitutor = substitutor

        // Maps the type variable `TypeVariable(A)` into the resulting type
        val systemSubstitutor = system.asReadOnlyStorage().buildCurrentSubstitutor(system, emptyMap()) as ConeSubstitutor

        val typeMapping = typeArgumentMapping as? TypeArgumentMapping.Mapped
        return buildMap {
            for ((index, parameterSymbol) in typeParameters.withIndex()) {
                val explicitTypeArgument = typeMapping?.get(index) as? FirTypeProjectionWithVariance
                if (explicitTypeArgument != null) {
                    put(parameterSymbol, explicitTypeArgument.typeRef.coneType)
                    continue
                }

                val parameterType = parameterSymbol.toConeType()
                val typeVariable = candidateSubstitutor.substituteOrNull(parameterType) ?: continue
                val resultingType = systemSubstitutor.substituteOrNull(typeVariable) ?: continue
                put(parameterSymbol, resultingType)
            }
        }
    }

    private fun createKtCall(
        psi: KtElement,
        fir: FirElement,
        calleeReference: FirReference,
        candidate: Candidate?,
        resolveFragmentOfCall: Boolean,
    ): KaCall? {
        val targetSymbol = candidate?.symbol
            ?: calleeReference.toResolvedBaseSymbol()
            ?: return null
        if (targetSymbol !is FirCallableSymbol<*>) return null
        if (targetSymbol is FirErrorFunctionSymbol || targetSymbol is FirErrorPropertySymbol) return null

        val firTypeArgumentsMapping = candidate?.toFirTypeArgumentsMapping(targetSymbol) ?: when (fir) {
            is FirQualifiedAccessExpression -> fir.toFirTypeArgumentsMapping(targetSymbol)
            is FirVariableAssignment -> fir.unwrapLValue()?.toFirTypeArgumentsMapping(targetSymbol).orEmpty()
            is FirDelegatedConstructorCall -> fir.toFirTypeArgumentsMapping(targetSymbol)
            else -> emptyMap()
        }

        val typeArgumentsMapping = firTypeArgumentsMapping.asKaTypeParametersMapping()

        handleCompoundAccessCall(psi, fir, resolveFragmentOfCall, typeArgumentsMapping)?.let { return it }

        val signature = with(analysisSession) {
            val substitutor = substitutorByMap(firTypeArgumentsMapping, firSession).toKtSubstitutor()

            // This is crucial to create a signature by Fir symbol as it can be call-site substitution
            val unsubstitutedSignature = targetSymbol.toKaSignature()
            unsubstitutedSignature.substitute(substitutor)
        }

        var firstArgIsExtensionReceiver = false
        var isImplicitInvoke = false

        fun createKtPartiallyAppliedSymbolForImplicitInvoke(
            dispatchReceiver: FirExpression?,
            extensionReceiver: FirExpression?,
            explicitReceiverKind: ExplicitReceiverKind,
            contextArguments: List<FirExpression>,
        ): KaPartiallyAppliedSymbol<KaCallableSymbol, KaCallableSignature<KaCallableSymbol>> {
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

            val dispatchReceiverValue: KaReceiverValue?
            val extensionReceiverValue: KaReceiverValue?
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

                    dispatchReceiverValue = KaBaseExplicitReceiverValue(
                        expression = explicitReceiverPsi,
                        backingType = dispatchReceiver.resolvedType.asKtType(),
                        isSafeNavigation = false,
                    )

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
                    extensionReceiverValue = KaBaseExplicitReceiverValue(
                        expression = explicitReceiverPsi,
                        backingType = extensionReceiver.resolvedType.asKtType(),
                        isSafeNavigation = false,
                    )
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
            return KaBasePartiallyAppliedSymbol(
                backingSignature = signature,
                dispatchReceiver = dispatchReceiverValue,
                extensionReceiver = extensionReceiverValue,
                contextArguments = contextArguments.toKaContextParameterValues(),
            )
        }

        val partiallyAppliedSymbol = when {
            candidate != null -> when {
                fir is FirImplicitInvokeCall ||
                        calleeReference.calleeOrCandidateName != OperatorNameConventions.INVOKE && targetSymbol.isInvokeFunction() -> {

                    // Implicit invoke (e.g., `x()`) will have a different callee symbol (e.g., `x`) than the candidate (e.g., `invoke`).
                    createKtPartiallyAppliedSymbolForImplicitInvoke(
                        dispatchReceiver = candidate.dispatchReceiver?.expression,
                        extensionReceiver = candidate.chosenExtensionReceiver?.expression,
                        explicitReceiverKind = candidate.explicitReceiverKind,
                        contextArguments = candidate.contextArguments(),
                    )
                }

                else -> KaBasePartiallyAppliedSymbol(
                    backingSignature = signature,
                    dispatchReceiver = candidate.dispatchReceiver?.expression?.toKtReceiverValue(),
                    extensionReceiver = candidate.chosenExtensionReceiver?.expression?.toKtReceiverValue(),
                    contextArguments = candidate.contextArguments().toKaContextParameterValues(),
                )
            }

            fir is FirImplicitInvokeCall -> {
                val explicitReceiverKind = if (fir.explicitReceiver == fir.dispatchReceiver) {
                    ExplicitReceiverKind.DISPATCH_RECEIVER
                } else {
                    ExplicitReceiverKind.EXTENSION_RECEIVER
                }

                createKtPartiallyAppliedSymbolForImplicitInvoke(
                    dispatchReceiver = fir.dispatchReceiver,
                    extensionReceiver = fir.extensionReceiver,
                    explicitReceiverKind = explicitReceiverKind,
                    contextArguments = fir.contextArguments,
                )
            }

            fir is FirQualifiedAccessExpression -> KaBasePartiallyAppliedSymbol(
                backingSignature = signature,
                dispatchReceiver = fir.dispatchReceiver?.toKtReceiverValue(),
                extensionReceiver = fir.extensionReceiver?.toKtReceiverValue(),
                contextArguments = fir.contextArguments.toKaContextParameterValues(),
            )

            fir is FirVariableAssignment -> KaBasePartiallyAppliedSymbol(
                backingSignature = signature,
                dispatchReceiver = fir.dispatchReceiver?.toKtReceiverValue(),
                extensionReceiver = fir.extensionReceiver?.toKtReceiverValue(),
                contextArguments = fir.contextArguments.toKaContextParameterValues(),
            )

            fir is FirDelegatedConstructorCall -> KaBasePartiallyAppliedSymbol(
                backingSignature = signature,
                dispatchReceiver = fir.dispatchReceiver?.toKtReceiverValue(),
                extensionReceiver = null,
                contextArguments = fir.contextArguments.toKaContextParameterValues(),
            )

            else -> KaBasePartiallyAppliedSymbol(
                backingSignature = signature,
                dispatchReceiver = null,
                extensionReceiver = null,
                contextArguments = emptyList(),
            )
        }

        return when (fir) {
            is FirAnnotationCall -> {
                if (partiallyAppliedSymbol.symbol !is KaConstructorSymbol) return null
                @Suppress("UNCHECKED_CAST") // safe because of the above check on targetKtSymbol
                KaBaseAnnotationCall(
                    partiallyAppliedSymbol as KaPartiallyAppliedFunctionSymbol<KaConstructorSymbol>,
                    fir.createArgumentMapping(partiallyAppliedSymbol.signature as KaFunctionSignature<*>)
                )
            }
            is FirDelegatedConstructorCall -> {
                if (partiallyAppliedSymbol.symbol !is KaConstructorSymbol) return null
                @Suppress("UNCHECKED_CAST") // safe because of the above check on targetKtSymbol
                KaBaseDelegatedConstructorCall(
                    partiallyAppliedSymbol as KaPartiallyAppliedFunctionSymbol<KaConstructorSymbol>,
                    if (fir.isThis) KaDelegatedConstructorCall.Kind.THIS_CALL else KaDelegatedConstructorCall.Kind.SUPER_CALL,
                    fir.createArgumentMapping(partiallyAppliedSymbol.signature as KaFunctionSignature<*>),
                    typeArgumentsMapping,
                )
            }
            is FirVariableAssignment -> {
                if (partiallyAppliedSymbol.symbol !is KaVariableSymbol) return null
                val rhs = fir.rValue.psi as? KtExpression
                @Suppress("UNCHECKED_CAST") // safe because of the above check on targetKtSymbol
                KaBaseSimpleVariableAccessCall(
                    partiallyAppliedSymbol as KaPartiallyAppliedVariableSymbol<KaVariableSymbol>,
                    typeArgumentsMapping,
                    KaBaseSimpleVariableWriteAccess(rhs),
                )
            }
            is FirPropertyAccessExpression, is FirCallableReferenceAccess -> {
                when (partiallyAppliedSymbol.symbol) {
                    is KaVariableSymbol -> {
                        @Suppress("UNCHECKED_CAST") // safe because of the above check on targetKtSymbol
                        KaBaseSimpleVariableAccessCall(
                            partiallyAppliedSymbol as KaPartiallyAppliedVariableSymbol<KaVariableSymbol>,
                            typeArgumentsMapping,
                            KaBaseSimpleVariableReadAccess,
                        )
                    }
                    // if errorsness call without ()
                    is KaFunctionSymbol -> {
                        @Suppress("UNCHECKED_CAST") // safe because of the above check on targetKtSymbol
                        KaBaseSimpleFunctionCall(
                            partiallyAppliedSymbol as KaPartiallyAppliedFunctionSymbol<KaFunctionSymbol>,
                            emptyMap(),
                            typeArgumentsMapping,
                            isImplicitInvoke,
                        )
                    }
                }
            }
            is FirFunctionCall -> {
                if (partiallyAppliedSymbol.symbol !is KaFunctionSymbol) return null
                val argumentMapping = if (candidate is Candidate) {
                    runIf(candidate.argumentMappingInitialized) { candidate.argumentMapping.unwrapAtoms() }
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
                KaBaseSimpleFunctionCall(
                    partiallyAppliedSymbol as KaPartiallyAppliedFunctionSymbol<KaFunctionSymbol>,
                    argumentMappingWithoutExtensionReceiver
                        ?.createArgumentMapping(partiallyAppliedSymbol.signature)
                        ?: emptyMap(),
                    typeArgumentsMapping,
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

    /**
     * Handle compound assignment with array access convention
     */
    private fun createKaCallForArrayAccessConvention(
        fir: FirElement,
        accessExpression: KtExpression?,
        resolveFragmentOfCall: Boolean,
        contextProvider: (FirFunctionCall, KtArrayAccessExpression) -> CompoundArrayAccessContext?,
        compoundOperationProvider: (KaPartiallyAppliedFunctionSymbol<KaNamedFunctionSymbol>) -> KaCompoundOperation,
    ): KaCall? {
        if (fir !is FirFunctionCall || fir.calleeReference.name != OperatorNameConventions.SET || accessExpression !is KtArrayAccessExpression) {
            return null
        }

        val context = contextProvider(fir, accessExpression) ?: return null
        val getSymbol = context.getSymbol
        val getArgumentMapping = accessExpression.indexExpressions.zip(getSymbol.signature.valueParameters).toMap()
        return if (resolveFragmentOfCall) {
            KaBaseSimpleFunctionCall(
                backingPartiallyAppliedSymbol = getSymbol,
                argumentMapping = getArgumentMapping,
                typeArgumentsMapping = fir.toFirTypeArgumentsMapping(getSymbol.symbol.firSymbol).asKaTypeParametersMapping(),
                isImplicitInvoke = false,
            )
        } else {
            KaBaseCompoundArrayAccessCall(
                backingCompoundAccess = compoundOperationProvider(context.operationSymbol),
                indexArguments = accessExpression.indexExpressions,
                getPartiallyAppliedSymbol = getSymbol,
                setPartiallyAppliedSymbol = context.setSymbol
            )
        }
    }

    /**
     * Handle compound assignment with variable
     */
    private fun createKaCallForVariableAccessConvention(
        fir: FirElement,
        accessExpression: KtExpression?,
        resolveFragmentOfCall: Boolean,
        typeArgumentsMapping: Map<KaTypeParameterSymbol, KaType>,
        compoundOperationProvider: (KaPartiallyAppliedFunctionSymbol<KaNamedFunctionSymbol>) -> KaCompoundOperation,
    ): KaCall? {
        if (fir !is FirVariableAssignment || accessExpression !is KtQualifiedExpression && accessExpression !is KtNameReferenceExpression) {
            return null
        }

        val variableSymbol = fir.toPartiallyAppliedSymbol() ?: return null
        val operationSymbol = getOperationPartiallyAppliedSymbolsForCompoundVariableAccess(fir, accessExpression) ?: return null
        return if (resolveFragmentOfCall) {
            KaBaseSimpleVariableAccessCall(
                backingPartiallyAppliedSymbol = variableSymbol,
                typeArgumentsMapping = typeArgumentsMapping,
                simpleAccess = KaBaseSimpleVariableReadAccess,
            )
        } else {
            KaBaseCompoundVariableAccessCall(
                backingPartiallyAppliedSymbol = variableSymbol,
                compoundAccess = compoundOperationProvider(operationSymbol),
            )
        }
    }

    private fun createKaCallForCompoundAccessConvention(
        fir: FirElement,
        accessExpression: KtExpression?,
        resolveFragmentOfCall: Boolean,
        typeArgumentsMapping: Map<KaTypeParameterSymbol, KaType>,
        contextProvider: (FirFunctionCall, KtArrayAccessExpression) -> CompoundArrayAccessContext?,
        compoundOperationProvider: (KaPartiallyAppliedFunctionSymbol<KaNamedFunctionSymbol>) -> KaCompoundOperation,
    ): KaCall? = createKaCallForArrayAccessConvention(
        fir = fir,
        accessExpression = accessExpression,
        resolveFragmentOfCall = resolveFragmentOfCall,
        contextProvider = contextProvider,
        compoundOperationProvider = compoundOperationProvider
    ) ?: createKaCallForVariableAccessConvention(
        fir = fir,
        accessExpression = accessExpression,
        resolveFragmentOfCall = resolveFragmentOfCall,
        typeArgumentsMapping = typeArgumentsMapping,
        compoundOperationProvider = compoundOperationProvider
    )

    private fun handleCompoundAccessCall(
        psi: KtElement,
        fir: FirElement,
        resolveFragmentOfCall: Boolean,
        typeArgumentsMapping: Map<KaTypeParameterSymbol, KaType>,
    ): KaCall? {
        return when (psi) {
            is KtBinaryExpression if psi.operationToken in KtTokens.AUGMENTED_ASSIGNMENTS -> {
                val rightOperandPsi = deparenthesize(psi.right) ?: return null
                val leftOperandPsi = deparenthesize(psi.left) ?: return null
                val compoundAssignKind = psi.getCompoundAssignKind()

                createKaCallForCompoundAccessConvention(
                    fir = fir,
                    accessExpression = leftOperandPsi,
                    resolveFragmentOfCall = resolveFragmentOfCall,
                    typeArgumentsMapping = typeArgumentsMapping,
                    contextProvider = ::getOperationPartiallyAppliedSymbolsForCompoundArrayAssignment,
                    compoundOperationProvider = { KaBaseCompoundAssignOperation(it, compoundAssignKind, rightOperandPsi) },
                )
            }

            is KtUnaryExpression if psi.operationToken in KtTokens.INCREMENT_AND_DECREMENT -> {
                val precedence = when (psi) {
                    is KtPostfixExpression -> KaCompoundUnaryOperation.Precedence.POSTFIX
                    else -> KaCompoundUnaryOperation.Precedence.PREFIX
                }

                val incOrDecOperationKind = psi.getInOrDecOperationKind()
                val baseExpression = deparenthesize(psi.baseExpression)

                createKaCallForCompoundAccessConvention(
                    fir = fir,
                    accessExpression = baseExpression,
                    resolveFragmentOfCall = resolveFragmentOfCall,
                    typeArgumentsMapping = typeArgumentsMapping,
                    contextProvider = { firCall, ktExpression ->
                        getOperationPartiallyAppliedSymbolsForIncOrDecOperation(firCall, ktExpression, precedence)
                    },
                    compoundOperationProvider = { KaBaseCompoundUnaryOperation(it, incOrDecOperationKind, precedence) },
                )
            }
            else -> null
        }
    }

    private class CompoundArrayAccessContext(
        val operationSymbol: KaPartiallyAppliedFunctionSymbol<KaNamedFunctionSymbol>,
        val getSymbol: KaPartiallyAppliedFunctionSymbol<KaNamedFunctionSymbol>,
        val setSymbol: KaPartiallyAppliedFunctionSymbol<KaNamedFunctionSymbol>,
    )

    private fun getOperationPartiallyAppliedSymbolsForCompoundArrayAssignment(
        fir: FirFunctionCall,
        arrayAccessExpression: KtArrayAccessExpression,
    ): CompoundArrayAccessContext? {
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

        return CompoundArrayAccessContext(
            operationPartiallyAppliedSymbol,
            getPartiallyAppliedSymbol,
            setPartiallyAppliedSymbol
        )
    }

    private fun getOperationPartiallyAppliedSymbolsForIncOrDecOperation(
        fir: FirFunctionCall,
        arrayAccessExpression: KtArrayAccessExpression,
        incDecPrecedence: KaCompoundUnaryOperation.Precedence,
    ): CompoundArrayAccessContext? {
        val lastArg = fir.arguments.lastOrNull() ?: return null
        val setPartiallyAppliedSymbol = fir.toPartiallyAppliedSymbol(arrayAccessExpression.arrayExpression) ?: return null
        return when (incDecPrecedence) {
            KaCompoundUnaryOperation.Precedence.PREFIX -> {
                // For prefix case, the last argument is a call to get(...).inc().
                val operationCall = lastArg as? FirFunctionCall ?: return null
                val operationPartiallyAppliedSymbol = operationCall.toPartiallyAppliedSymbol(arrayAccessExpression) ?: return null
                // The get call is the explicit receiver of this operation call
                val getCall = operationCall.explicitReceiver as? FirFunctionCall ?: return null
                val getPartiallyAppliedSymbol = getCall.toPartiallyAppliedSymbol(arrayAccessExpression.arrayExpression) ?: return null
                CompoundArrayAccessContext(
                    operationPartiallyAppliedSymbol,
                    getPartiallyAppliedSymbol,
                    setPartiallyAppliedSymbol
                )
            }

            KaCompoundUnaryOperation.Precedence.POSTFIX -> {
                // For postfix case, the last argument is the operation call invoked on a synthetic local variable `<unary>`. This local
                // variable is initialized by calling the `get` function.
                val operationCall = lastArg as? FirFunctionCall ?: return null
                val operationPartiallyAppliedSymbol = operationCall.toPartiallyAppliedSymbol(arrayAccessExpression) ?: return null
                val receiverOfOperationCall = operationCall.explicitReceiver ?: return null
                val getCall = getInitializerOfReferencedLocalVariable(receiverOfOperationCall)
                val getPartiallyAppliedSymbol = getCall?.toPartiallyAppliedSymbol(arrayAccessExpression.arrayExpression) ?: return null
                CompoundArrayAccessContext(
                    operationPartiallyAppliedSymbol,
                    getPartiallyAppliedSymbol,
                    setPartiallyAppliedSymbol
                )
            }
        }
    }

    @OptIn(SymbolInternals::class)
    private fun getInitializerOfReferencedLocalVariable(variableReference: FirExpression): FirFunctionCall? {
        return variableReference.toReference(resolutionFacade.useSiteFirSession)
            ?.toResolvedVariableSymbol()
            ?.fir
            ?.initializer as? FirFunctionCall
    }

    private fun getOperationPartiallyAppliedSymbolsForCompoundVariableAccess(
        fir: FirVariableAssignment,
        leftOperandPsi: KtExpression,
    ): KaPartiallyAppliedFunctionSymbol<KaNamedFunctionSymbol>? {
        // The new value is a call to the appropriate operator function.
        val operationCall = fir.rValue as? FirFunctionCall ?: getInitializerOfReferencedLocalVariable(fir.rValue) ?: return null
        return operationCall.toPartiallyAppliedSymbol(leftOperandPsi)
    }

    private fun FirVariableAssignment.toPartiallyAppliedSymbol(): KaPartiallyAppliedVariableSymbol<KaVariableSymbol>? {
        val variableRef = calleeReference as? FirResolvedNamedReference ?: return null
        val variableSymbol = variableRef.resolvedSymbol as? FirVariableSymbol<*> ?: return null
        val substitutor = unwrapLValue()?.createConeSubstitutorFromTypeArguments(rootModuleSession) ?: return null
        val ktSignature = variableSymbol.toKaSignature()
        return KaBasePartiallyAppliedSymbol(
            backingSignature = with(analysisSession) { ktSignature.substitute(substitutor.toKtSubstitutor()) },
            dispatchReceiver = dispatchReceiver?.toKtReceiverValue(),
            extensionReceiver = extensionReceiver?.toKtReceiverValue(),
            contextArguments = contextArguments.toKaContextParameterValues(),
        )
    }

    private fun FirFunctionCall.toPartiallyAppliedSymbol(
        explicitReceiverPsiSupplement: KtExpression? = null,
    ): KaPartiallyAppliedFunctionSymbol<KaNamedFunctionSymbol>? {
        val operationSymbol =
            (calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirNamedFunctionSymbol ?: return null
        val substitutor = createConeSubstitutorFromTypeArguments(rootModuleSession) ?: return null
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
        val ktSignature = operationSymbol.toKaSignature()
        return KaBasePartiallyAppliedSymbol(
            backingSignature = with(analysisSession) { ktSignature.substitute(substitutor.toKtSubstitutor()) },
            dispatchReceiver = dispatchReceiverValue,
            extensionReceiver = extensionReceiverValue,
            contextArguments = contextArguments.toKaContextParameterValues(),
        )
    }

    private fun List<FirExpression>.toKaContextParameterValues(): List<KaReceiverValue> {
        return mapNotNull { expression ->
            val receiverValue = expression.toKtReceiverValue()
            if (receiverValue == null) {
                logger<KaFirResolver>().logErrorWithAttachment("Unexpected null receiver value for context argument") {
                    withFirEntry("expression", expression)
                }
            }

            receiverValue
        }
    }

    private fun FirExpression.toKtReceiverValue(): KaReceiverValue? {
        return when (this) {
            is FirSmartCastExpression -> {
                val result = originalExpression.toKtReceiverValue()
                if (result != null && isStable) {
                    KaBaseSmartCastedReceiverValue(result, smartcastType.coneType.asKtType())
                } else {
                    result
                }
            }

            is FirThisReceiverExpression if isImplicit -> {
                val symbol = when (val firSymbol = calleeReference.boundSymbol) {
                    is FirClassSymbol<*> -> firSymbol.toKaSymbol()
                    is FirReceiverParameterSymbol -> firSymbolBuilder.callableBuilder.buildExtensionReceiverSymbol(firSymbol)
                        ?: return null

                    is FirValueParameterSymbol -> firSymbolBuilder.variableBuilder.buildParameterSymbol(firSymbol)
                    is FirTypeAliasSymbol, is FirTypeParameterSymbol -> errorWithFirSpecificEntries(
                        message = "Unexpected ${FirThisOwnerSymbol::class.simpleName}: ${firSymbol::class.simpleName}",
                        fir = firSymbol.fir
                    )

                    null -> return null
                }

                KaBaseImplicitReceiverValue(symbol, resolvedType.asKtType())
            }

            is FirPropertyAccessExpression if source?.kind is KtFakeSourceElementKind.ImplicitContextParameterArgument -> {
                val firSymbol = calleeReference.symbol ?: return null

                require(firSymbol is FirValueParameterSymbol) { "Unexpected symbol ${firSymbol::class.simpleName}" }
                val symbol = firSymbolBuilder.variableBuilder.buildParameterSymbol(firSymbol)
                KaBaseImplicitReceiverValue(symbol, resolvedType.asKtType())
            }

            is FirResolvedQualifier if this.source?.kind is KtFakeSourceElementKind.ImplicitReceiver -> {
                val symbol = this.symbol ?: return null
                KaBaseImplicitReceiverValue(symbol.toKaSymbol(), resolvedType.asKtType())
            }

            else -> {
                val psi = psi
                if (psi !is KtExpression) return null
                psi.toExplicitReceiverValue(resolvedType.asKtType())
            }
        }
    }

    private fun FirCallableSymbol<*>.toKaSignature(): KaCallableSignature<KaCallableSymbol> =
        firSymbolBuilder.callableBuilder.buildCallableSignature(this)

    private fun FirClassLikeSymbol<*>.toKaSymbol(): KaClassLikeSymbol = firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(this)

    private fun FirNamedFunctionSymbol.toKaSignature(): KaFunctionSignature<KaNamedFunctionSymbol> =
        firSymbolBuilder.functionBuilder.buildNamedFunctionSignature(this)

    private fun FirVariableSymbol<*>.toKaSignature(): KaVariableSignature<KaVariableSymbol> =
        firSymbolBuilder.variableBuilder.buildVariableLikeSignature(this)

    private fun FirQualifiedAccessExpression.toFirTypeArgumentsMapping(symbol: FirCallableSymbol<*>): Map<FirTypeParameterSymbol, ConeKotlinType> {
        return toFirTypeArgumentsMapping(typeArguments, symbol)
    }

    private fun FirResolvedQualifier.toFirTypeArgumentsMapping(symbol: FirCallableSymbol<*>): Map<FirTypeParameterSymbol, ConeKotlinType> {
        return toFirTypeArgumentsMapping(typeArguments, symbol)
    }

    private fun FirDelegatedConstructorCall.toFirTypeArgumentsMapping(symbol: FirCallableSymbol<*>): Map<FirTypeParameterSymbol, ConeKotlinType> {
        val typeParameters = symbol.typeParameterSymbols.ifEmpty { return emptyMap() }
        val typeArguments = constructedTypeRef.coneType.typeArguments
        // In all cases, the size of arguments and parameters is the same,
        // so this check exists just to be sure
        if (typeArguments.size != typeParameters.size) return emptyMap()

        return buildMap(typeArguments.size) {
            for ((index, projection) in typeArguments.withIndex()) {
                if (projection !is ConeKotlinType) return emptyMap()
                put(typeParameters[index], projection)
            }
        }
    }

    /**
     * Maps [typeArguments] to the type parameters of [symbol].
     *
     * If too many type arguments are provided, a mapping is still created. Extra type arguments are simply ignored. If this wasn't the
     * case, the resulting [KaCall] would contain no type arguments at all, which can cause problems later. If too few type arguments are
     * provided, an empty map is returned defensively so that [toFirTypeArgumentsMapping] doesn't conjure any error types. If you want to map
     * too few type arguments meaningfully, please provide filler types explicitly.
     */
    private fun toFirTypeArgumentsMapping(
        typeArguments: List<FirTypeProjection>,
        symbol: FirCallableSymbol<*>,
    ): Map<FirTypeParameterSymbol, ConeKotlinType> {
        val typeParameters = symbol.typeParameterSymbols
        if (typeParameters.isEmpty()) return emptyMap()
        if (typeArguments.size < typeParameters.size) return emptyMap()

        val result = mutableMapOf<FirTypeParameterSymbol, ConeKotlinType>()

        for ((index, typeParameter) in typeParameters.withIndex()) {
            // After resolution all type arguments should be usual types (not FirPlaceholderProjection)
            val typeArgument = typeArguments[index]
            if (typeArgument !is FirTypeProjectionWithVariance || typeArgument.variance != Variance.INVARIANT) return emptyMap()
            result[typeParameter] = typeArgument.typeRef.coneType
        }

        return result
    }

    private fun FirArrayLiteral.toTypeArgumentsMapping(symbol: KaDeclarationSymbol): Map<KaTypeParameterSymbol, KaType> {
        val elementType = resolvedType.arrayElementType()?.asKtType() ?: return emptyMap()
        val typeParameter = symbol.typeParameters.singleOrNull() ?: return emptyMap()
        return mapOf(typeParameter to elementType)
    }

    // TODO: Refactor common code with FirElement.toKtCallInfo() when other FirResolvables are handled
    private fun FirElement.collectCallCandidates(
        psi: KtElement,
        resolveCalleeExpressionOfFunctionCall: Boolean,
        resolveFragmentOfCall: Boolean,
    ): List<KaCallCandidateInfo> {
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
            is FirFunctionCall, is FirPropertyAccessExpression -> collectCallCandidates(psi, resolveFragmentOfCall)
            is FirSafeCallExpression -> selector.collectCallCandidates(
                psi = psi,
                resolveCalleeExpressionOfFunctionCall = resolveCalleeExpressionOfFunctionCall,
                resolveFragmentOfCall = resolveFragmentOfCall,
            )

            is FirComparisonExpression -> compareToCall.collectCallCandidates(
                psi = psi,
                resolveCalleeExpressionOfFunctionCall = resolveCalleeExpressionOfFunctionCall,
                resolveFragmentOfCall = resolveFragmentOfCall,
            )

            is FirResolvedQualifier -> toKtCallCandidateInfos()
            is FirDelegatedConstructorCall -> collectCallCandidatesForDelegatedConstructorCall(psi, resolveFragmentOfCall)
            else -> toKtCallInfo(psi, resolveCalleeExpressionOfFunctionCall, resolveFragmentOfCall).toKtCallCandidateInfos()
        }
    }

    private fun FirResolvedQualifier.toKtCallCandidateInfos(): List<KaCallCandidateInfo> {
        return toKtCalls(findQualifierConstructors()).map {
            KaBaseInapplicableCallCandidateInfo(
                it,
                isInBestCandidates = false,
                diagnostic = inapplicableCandidateDiagnostic()
            )
        }
    }

    private fun FirResolvedQualifier.findQualifierConstructors(): List<FirConstructorSymbol> {
        val classSymbol = this.symbol?.fullyExpandedClass(analysisSession.firSession) ?: return emptyList()
        return classSymbol.unsubstitutedScope(
            analysisSession.firSession,
            analysisSession.getScopeSessionFor(analysisSession.firSession),
            withForcedTypeCalculator = true,
            memberRequiredPhase = null,
        ).getDeclaredConstructors()
    }

    private fun Map<FirTypeParameterSymbol, ConeKotlinType>.asKaTypeParametersMapping(): Map<KaTypeParameterSymbol, KaType> {
        return map { (key, value) ->
            firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(key) to value.asKtType()
        }.toMap()
    }

    private fun FirResolvedQualifier.toKtCalls(constructors: List<FirConstructorSymbol>): List<KaCall> {
        analysisSession.apply {
            return constructors.map { constructor ->
                val signature = constructor.toKaSignature()
                val partiallyAppliedSymbol = KaBasePartiallyAppliedSymbol(
                    backingSignature = signature as KaFunctionSignature<*>,
                    dispatchReceiver = null,
                    extensionReceiver = null,
                    contextArguments = emptyList(),
                )

                val firTypeArgumentsMapping = toFirTypeArgumentsMapping(constructor)
                val typeArgumentsMapping = firTypeArgumentsMapping.asKaTypeParametersMapping()
                KaBaseSimpleFunctionCall(partiallyAppliedSymbol, emptyMap(), typeArgumentsMapping, false)
            }
        }
    }

    private fun FirQualifiedAccessExpression.collectCallCandidates(
        psi: KtElement,
        resolveFragmentOfCall: Boolean,
    ): List<KaCallCandidateInfo> {
        // If a function call is resolved to an implicit invoke call, the FirImplicitInvokeCall will have the `invoke()` function as the
        // callee and the variable as the explicit receiver. To correctly get all candidates, we need to get the original function
        // call's explicit receiver (if there is any) and callee (i.e., the variable).
        val unwrappedExplicitReceiver = explicitReceiver?.unwrapSmartcastExpression()
        val isUnwrappedImplicitInvokeCall = this is FirImplicitInvokeCall && unwrappedExplicitReceiver is FirPropertyAccessExpression
        val originalFunctionCall = if (isUnwrappedImplicitInvokeCall) {
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
        val candidates = AllCandidatesResolver(analysisSession.firSession).getAllCandidates(
            analysisSession.resolutionFacade,
            originalFunctionCall,
            calleeName,
            psi,
            ResolutionMode.ContextIndependent,
        )

        return candidates.mapNotNull {
            convertToKaCallCandidateInfo(
                resolvable = originalFunctionCall,
                element = psi,
                candidate = it.candidate,
                isInBestCandidates = it.isInBestCandidates,
                resolveFragmentOfCall = resolveFragmentOfCall,
                isUnwrappedImplicitInvokeCall = isUnwrappedImplicitInvokeCall,
            )
        }
    }

    private fun FirDelegatedConstructorCall.collectCallCandidatesForDelegatedConstructorCall(
        psi: KtElement,
        resolveFragmentOfCall: Boolean,
    ): List<KaCallCandidateInfo> {
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
            .getAllCandidatesForDelegatedConstructor(analysisSession.resolutionFacade, this, derivedClass.toLookupTag(), psi)

        return candidates.mapNotNull {
            convertToKaCallCandidateInfo(
                resolvable = this,
                element = psi,
                candidate = it.candidate,
                isInBestCandidates = it.isInBestCandidates,
                resolveFragmentOfCall = resolveFragmentOfCall,
                isUnwrappedImplicitInvokeCall = false,
            )
        }
    }

    private fun KaCallInfo?.toKtCallCandidateInfos(): List<KaCallCandidateInfo> {
        return when (this) {
            is KaSuccessCallInfo -> listOf(KaBaseApplicableCallCandidateInfo(call, isInBestCandidates = true))
            is KaErrorCallInfo -> candidateCalls.map { KaBaseInapplicableCallCandidateInfo(it, isInBestCandidates = true, diagnostic) }
            null -> emptyList()
        }
    }

    private fun convertToKaCallCandidateInfo(
        resolvable: FirResolvable,
        element: KtElement,
        candidate: Candidate,
        isInBestCandidates: Boolean,
        resolveFragmentOfCall: Boolean,
        isUnwrappedImplicitInvokeCall: Boolean,
    ): KaCallCandidateInfo? {
        val call = createKtCall(element, resolvable, candidate, resolveFragmentOfCall) ?: return null

        if (candidate.isSuccessful) {
            return KaBaseApplicableCallCandidateInfo(
                call,
                isInBestCandidates = if (isUnwrappedImplicitInvokeCall) {
                    (call as? KaSimpleFunctionCall)?.isImplicitInvoke == true
                } else {
                    isInBestCandidates
                }
            )
        }

        val diagnostic = createConeDiagnosticForCandidateWithError(candidate.lowestApplicability, candidate)
        if (diagnostic is ConeHiddenCandidateError) return null
        val ktDiagnostic =
            resolvable.source?.let { diagnostic.asKtDiagnostic(it, element.toKtPsiSourceElement()) }
                ?: KaNonBoundToPsiErrorDiagnostic(factoryName = FirErrors.OTHER_ERROR.name, diagnostic.reason, token)
        return KaBaseInapplicableCallCandidateInfo(call, isInBestCandidates, ktDiagnostic)
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

    private fun FirArrayLiteral.toKtCallInfo(): KaCallInfo? {
        val arrayOfSymbol = with(analysisSession) {

            val type = resolvedType as? ConeClassLikeType
                ?: return run {
                    val defaultArrayOfSymbol = arrayOfSymbol(arrayOf) ?: return null
                    val substitutor = createSubstitutorFromTypeArguments(defaultArrayOfSymbol)
                    val partiallyAppliedSymbol = KaBasePartiallyAppliedSymbol(
                        backingSignature = with(useSiteSession) { defaultArrayOfSymbol.substitute(substitutor) },
                        dispatchReceiver = null,
                        extensionReceiver = null,
                        contextArguments = emptyList(),
                    )

                    KaBaseErrorCallInfo(
                        listOf(
                            KaBaseSimpleFunctionCall(
                                partiallyAppliedSymbol,
                                createArgumentMapping(defaultArrayOfSymbol, substitutor),
                                this@toKtCallInfo.toTypeArgumentsMapping(defaultArrayOfSymbol),
                                false,
                            )
                        ),
                        KaNonBoundToPsiErrorDiagnostic(
                            factoryName = FirErrors.OTHER_ERROR.name,
                            defaultMessage = "type of arrayOf call is not resolved",
                            token = token
                        ),
                    )
                }
            val call = arrayTypeToArrayOfCall[type.lookupTag.classId] ?: arrayOf
            arrayOfSymbol(call)
        } ?: return null
        val substitutor = createSubstitutorFromTypeArguments(arrayOfSymbol)
        val partiallyAppliedSymbol = KaBasePartiallyAppliedSymbol(
            backingSignature = with(analysisSession) { arrayOfSymbol.substitute(substitutor) },
            dispatchReceiver = null,
            extensionReceiver = null,
            contextArguments = emptyList(),
        )

        return KaBaseSuccessCallInfo(
            KaBaseSimpleFunctionCall(
                partiallyAppliedSymbol,
                createArgumentMapping(arrayOfSymbol, substitutor),
                this@toKtCallInfo.toTypeArgumentsMapping(arrayOfSymbol),
                false
            )
        )
    }

    private fun FirArrayLiteral.createSubstitutorFromTypeArguments(arrayOfSymbol: KaNamedFunctionSymbol): KaSubstitutor {
        val firSymbol = arrayOfSymbol.firSymbol
        // No type parameter means this is an arrayOf call of primitives, in which case there is no type arguments
        val typeParameter = firSymbol.fir.typeParameters.singleOrNull() ?: return KaSubstitutor.Empty(token)

        val elementType = resolvedType.arrayElementType() ?: return KaSubstitutor.Empty(token)
        val coneSubstitutor = substitutorByMap(mapOf(typeParameter.symbol to elementType), rootModuleSession)
        return firSymbolBuilder.typeBuilder.buildSubstitutor(coneSubstitutor)
    }

    private fun FirEqualityOperatorCall.toKtCallInfo(psi: KtElement): KaCallInfo? {
        val binaryExpression = deparenthesize(psi as? KtExpression) as? KtBinaryExpression ?: return null
        val leftPsi = binaryExpression.left ?: return null
        val rightPsi = binaryExpression.right ?: return null
        return when (operation) {
            FirOperation.EQ, FirOperation.NOT_EQ -> {
                val leftOperand = arguments.firstOrNull() ?: return null

                val equalsSymbol = getEqualsSymbol() ?: return null
                val kaSignature = equalsSymbol.toKaSignature()
                KaBaseSuccessCallInfo(
                    KaBaseSimpleFunctionCall(
                        KaBasePartiallyAppliedSymbol(
                            backingSignature = kaSignature,
                            dispatchReceiver = KaBaseExplicitReceiverValue(
                                expression = leftPsi,
                                backingType = leftOperand.resolvedType.asKtType(),
                                isSafeNavigation = false,
                            ),
                            extensionReceiver = null,
                            contextArguments = emptyList(),
                        ),
                        mapOf(rightPsi to kaSignature.valueParameters.first()),
                        emptyMap(),
                        false
                    )
                )
            }
            else -> null
        }
    }

    private fun FirEqualityOperatorCall.getEqualsSymbol(): FirNamedFunctionSymbol? {
        var equalsSymbol: FirNamedFunctionSymbol? = null
        processEqualsFunctions(analysisSession.firSession, analysisSession) {
            if (equalsSymbol != null) return@processEqualsFunctions
            equalsSymbol = it
        }

        return equalsSymbol ?: equalsSymbolInAny
    }

    private fun FirCall.createArgumentMapping(signatureOfCallee: KaFunctionSignature<*>): Map<KtExpression, KaVariableSignature<KaValueParameterSymbol>> {
        return resolvedArgumentMapping?.entries.createArgumentMapping(signatureOfCallee)
    }

    private fun Collection<MutableMap.MutableEntry<FirExpression, FirValueParameter>>?.createArgumentMapping(
        signatureOfCallee: KaFunctionSignature<*>,
    ): Map<KtExpression, KaVariableSignature<KaValueParameterSymbol>> {
        if (this == null || isEmpty()) return emptyMap()

        val paramSignatureByName = signatureOfCallee.valueParameters.associateBy {
            // We intentionally use `symbol.name` instead of `name` here, since
            // `FirValueParameter.name` is not affected by the `@ParameterName`
            it.symbol.name
        }

        val ktArgumentMapping = LinkedHashMap<KtExpression, KaVariableSignature<KaValueParameterSymbol>>(size)
        this.forEach { (firExpression, firValueParameter) ->
            val parameterSymbol = paramSignatureByName[firValueParameter.name] ?: return@forEach
            mapArgumentExpressionToParameter(firExpression, parameterSymbol, ktArgumentMapping)
        }

        return ktArgumentMapping
    }

    private fun FirArrayLiteral.createArgumentMapping(
        arrayOfSymbol: KaNamedFunctionSymbol,
        substitutor: KaSubstitutor,
    ): Map<KtExpression, KaVariableSignature<KaValueParameterSymbol>> {
        val arguments = argumentList.arguments
        if (arguments.isEmpty()) return emptyMap()

        val ktArgumentMapping = LinkedHashMap<KtExpression, KaVariableSignature<KaValueParameterSymbol>>(arguments.size)
        val parameterSymbol = arrayOfSymbol.valueParameters.single()

        for (firExpression in arguments) {
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
        parameterSymbol: KaVariableSignature<KaValueParameterSymbol>,
        argumentMapping: LinkedHashMap<KtExpression, KaVariableSignature<KaValueParameterSymbol>>,
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
            is FirNamedArgumentExpression, is FirSpreadArgumentExpression ->
                realPsi.safeAs<KtValueArgument>()?.getArgumentExpression()
            is FirAnonymousFunctionExpression ->
                realPsi?.parent as? KtLabeledExpression ?: realPsi as? KtExpression
            is FirWhenSubjectExpression ->
                // The subject variable is not processed here as we don't have KtExpression to represent it.
                // K1 creates a fake expression in this case.
                whenSubject?.findSourceKtExpressionForCallArgument()
            // FirBlock is a fake container for desugared expressions like `++index` or `++list[0]`
            is FirBlock -> psi as? KtExpression
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
                element.getOrBuildFir(resolutionFacade)?.let { withFirEntry("fir", it) }
            }
        }

    }

    private fun FirDiagnosticHolder.createKtDiagnostic(psi: KtElement?): KaDiagnostic {
        return (source?.let { diagnostic.asKtDiagnostic(it, psi?.toKtPsiSourceElement()) }
            ?: KaNonBoundToPsiErrorDiagnostic(factoryName = FirErrors.OTHER_ERROR.name, diagnostic.reason, token))
    }
}
