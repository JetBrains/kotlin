/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.openapi.diagnostic.logger
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostic
import org.jetbrains.kotlin.analysis.api.fir.*
import org.jetbrains.kotlin.analysis.api.fir.references.ClassicKDocReferenceResolver
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirArrayOfSymbolProvider.arrayOfSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.processEqualsFunctions
import org.jetbrains.kotlin.analysis.api.fir.utils.withSymbolAttachment
import org.jetbrains.kotlin.analysis.api.getModule
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseResolver
import org.jetbrains.kotlin.analysis.api.impl.base.components.withPsiValidityAssertion
import org.jetbrains.kotlin.analysis.api.impl.base.resolution.*
import org.jetbrains.kotlin.analysis.api.impl.base.util.KaNonBoundToPsiErrorDiagnostic
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
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
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnosticHolder
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCall
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.resolve.FirResolvedSymbolOrigin
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.stages.TypeArgumentMapping
import org.jetbrains.kotlin.fir.resolve.createConeDiagnosticForCandidateWithError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeDiagnosticWithCandidates
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeHiddenCandidateError
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.toArrayOfFactoryName
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
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.idea.references.KDocReference
import org.jetbrains.kotlin.idea.references.KtDefaultAnnotationArgumentReference
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.KtPsiUtil.deparenthesize
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getPossiblyQualifiedCallExpression
import org.jetbrains.kotlin.psi.psiUtil.topParenthesizedParentOrMe
import org.jetbrains.kotlin.resolve.ArrayFqNames
import org.jetbrains.kotlin.resolve.calls.inference.buildCurrentSubstitutor
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.toKtPsiSourceElement
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.util.OperatorNameConventions.EQUALS
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

        return wholeQualifier.resolvedToCompanionObject
    }

    override val KtReference.usesContextSensitiveResolution: Boolean
        get() = withPsiValidityAssertion(element) {
            if (this !is KtSimpleNameReference) {
                return false
            }

            val fir = element.getOrBuildFir(analysisSession.resolutionFacade) ?: return false
            when (fir) {
                is FirResolvedTypeRef -> fir.resolvedSymbolOrigin == FirResolvedSymbolOrigin.ContextSensitive
                is FirResolvedQualifier -> fir.resolvedSymbolOrigin == FirResolvedSymbolOrigin.ContextSensitive
                else -> {
                    val firReference = fir.toReference(analysisSession.firSession) ?: return false
                    firReference.isContextSensitive
                }
            }
        }

    @KaNonPublicApi
    override fun KDocReference.resolveToSymbolWithClassicKDocResolver(): KaSymbol? = withValidityAssertion {
        val element = this.element
        val fullFqName = generateSequence(element) { it.parent as? KDocName }.last().getQualifiedNameAsFqName()
        val selectedFqName = element.getQualifiedNameAsFqName()
        return ClassicKDocReferenceResolver.resolveKdocFqName(
            analysisSession,
            selectedFqName,
            fullFqName,
            element,
        ).firstOrNull()
    }

    override fun performSymbolResolution(psi: KtElement): KaSymbolResolutionAttempt? = wrapError(psi) {
        analysisSession.cacheStorage.resolveSymbolCache.value.getOrPut(psi) {
            resolveSymbol(psi)
        }
    }

    /**
     * For destructuring declaration entries, [getOrBuildFir] returns [FirProperty] (a declaration).
     * The actual resolution target is in [FirProperty.initializer] (e.g., [FirComponentCall] or [FirErrorExpression]).
     */
    private fun KtElement.getOrBuildFirWithAdjustments(): FirElement? {
        return when (val fir = getOrBuildFir(resolutionFacade)) {
            is FirProperty if this is KtDestructuringDeclarationEntry -> fir.initializer
            else -> fir
        }
    }

    private fun resolveSymbol(psi: KtElement): KaSymbolResolutionAttempt? {
        return when (val unwrappedFir = psi.getOrBuildFirWithAdjustments()?.unwrapSafeCall()) {
            is FirDiagnosticHolder -> unwrappedFir.toKaSymbolResolutionError(psi)
            is FirResolvable -> unwrappedFir.toKaSymbolResolutionAttempt(psi)
            is FirCollectionLiteral -> unwrappedFir.toKaSymbolResolutionAttempt(psi)
            is FirVariableAssignment -> unwrappedFir.calleeReference?.toKaSymbolResolutionAttempt(psi)
            is FirResolvedQualifier -> unwrappedFir.toKaSymbolResolutionAttempt(psi)
            is FirReference -> unwrappedFir.toKaSymbolResolutionAttempt(psi)
            is FirReturnExpression -> unwrappedFir.toKaSymbolResolutionAttempt(psi)
            is FirWhileLoop if psi is KtForExpression -> resolveForLoopSymbols(unwrappedFir)
            else -> null
        }
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

    override fun performCallResolution(psi: KtElement): KaCallResolutionAttempt? = wrapError(psi) {
        analysisSession.cacheStorage.resolveCallCache.value.getOrPut(psi) {
            val attempts = resolveCall(
                psi,
                onError = { psiToResolve ->
                    listOf(
                        KaBaseCallResolutionError(
                            backedDiagnostic = createKaDiagnostic(psiToResolve),
                            backingCandidateCalls = emptyList(),
                        ),
                    )
                },
                onSuccess = { psiToResolve, resolveCalleeExpressionOfFunctionCall, resolveFragmentOfCall ->
                    listOfNotNull(
                        toKaResolutionAttempt(
                            psiToResolve,
                            resolveCalleeExpressionOfFunctionCall,
                            resolveFragmentOfCall,
                        )
                    )
                }
            )

            attempts.singleOrNull()
        }
    }

    override fun performCallCandidatesCollection(psi: KtElement): List<KaCallCandidate> = wrapError(psi) {
        resolveCall(
            psi,
            onError = { emptyList() },
            onSuccess = { psiToResolve, resolveCalleeExpressionOfFunctionCall, resolveFragmentOfCall ->
                collectCallCandidates(
                    psiToResolve,
                    resolveCalleeExpressionOfFunctionCall,
                    resolveFragmentOfCall
                )
            }
        )
    }

    private fun FirResolvable.toKaSymbolResolutionAttempt(psi: KtElement): KaSymbolResolutionAttempt? {
        val calleeReference = when {
            /**
             * FIR doesn't have a dedicated element for label in `super@Foo.something()` statement
             *
             * ```kotlin
             * interface A { fun a() {} }
             *
             * class Foo : A {
             *     fun foo() {
             *         super@F<caret>oo.a()
             *     }
             * }
             * ```
             */
            psi is KtLabelReferenceExpression && calleeReference is FirSuperReference && this is FirQualifiedAccessExpression -> {
                (dispatchReceiver as? FirThisReceiverExpression)?.calleeReference
            }

            else -> calleeReference
        }

        return calleeReference?.toKaSymbolResolutionAttempt(psi)
    }

    private fun FirReference.toKaSymbolResolutionAttempt(psi: KtElement): KaSymbolResolutionAttempt? {
        if (this is FirDiagnosticHolder) {
            return toKaSymbolResolutionError(psi)
        }

        val symbol = symbol?.buildSymbol(firSymbolBuilder) ?: return null
        return KaBaseSymbolResolutionSuccess(backingSymbol = symbol)
    }

    private fun FirDiagnosticHolder.toKaSymbolResolutionError(psi: KtElement): KaSymbolResolutionError =
        KaBaseSymbolResolutionError(
            backingDiagnostic = createKaDiagnostic(psi),
            backingCandidateSymbols = diagnostic.getCandidateSymbols().map(firSymbolBuilder::buildSymbol),
        )

    private fun FirCollectionLiteral.toKaSymbolResolutionAttempt(psi: KtElement): KaSymbolResolutionAttempt = with(analysisSession) {
        val resolvedType = resolvedType as? ConeClassLikeType
        if (resolvedType is ConeErrorType) {
            return KaBaseSymbolResolutionError(
                backingDiagnostic = createKaDiagnostic(
                    source = source,
                    coneDiagnostic = resolvedType.diagnostic,
                    psi = psi,
                ),
                backingCandidateSymbols = emptyList(),
            )
        }

        val resolvedSymbol = arrayOfSymbol(this@toKaSymbolResolutionAttempt)
        if (resolvedSymbol != null) {
            return KaBaseSymbolResolutionSuccess(resolvedSymbol)
        }

        val defaultSymbol = arrayOfSymbol(ArrayFqNames.ARRAY_OF_FUNCTION)
        return KaBaseSymbolResolutionError(
            backingDiagnostic = unresolvedArrayOfDiagnostic,
            backingCandidateSymbols = listOfNotNull(defaultSymbol),
        )
    }

    private fun FirResolvedQualifier.toKaSymbolResolutionAttempt(psi: KtElement): KaSymbolResolutionAttempt? {
        if (psi !is KtCallExpression) {
            return null
        }

        val constructors = findQualifierConstructors()
        return KaBaseSymbolResolutionError(
            backingDiagnostic = inapplicableCandidateDiagnostic(),
            backingCandidateSymbols = constructors.map(firSymbolBuilder.functionBuilder::buildConstructorSymbol),
        )
    }

    private fun FirReturnExpression.toKaSymbolResolutionAttempt(
        psi: KtElement,
    ): KaSymbolResolutionAttempt = when (val firFunctionSymbol = target.labeledElement.symbol) {
        is FirErrorFunctionSymbol -> {
            val diagnostic = firFunctionSymbol.fir.createKaDiagnostic(psi)
            KaBaseSymbolResolutionError(backingCandidateSymbols = emptyList(), backingDiagnostic = diagnostic)
        }

        else -> {
            val kaSymbol = firFunctionSymbol.buildSymbol(firSymbolBuilder)
            KaBaseSymbolResolutionSuccess(kaSymbol)
        }
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

    private inline fun <T> resolveCall(
        psi: KtElement,
        onError: FirDiagnosticHolder.(psiToResolve: KtElement) -> List<T>,
        onSuccess: FirElement.(
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

        return when (val fir = psiToResolve.getOrBuildFirWithAdjustments()) {
            null -> emptyList()
            is FirDiagnosticHolder -> fir.onError(psiToResolve)
            else -> {
                val specialErrorCase = specialErrorCase(fir)
                specialErrorCase?.onError(psiToResolve) ?: fir.onSuccess(
                    psiToResolve,
                    psiToResolve == containingCallExpressionForCalleeExpression,
                    psiToResolve == containingBinaryExpressionForLhs || psiToResolve == containingUnaryExpressionForIncOrDec,
                )
            }
        }
    }

    /**
     * Some [FirElement] might not implement [FirDiagnosticHolder] directly, but still effectively hold diagnostics
     */
    private fun specialErrorCase(fir: FirElement): FirDiagnosticHolder? = when (fir) {
        is FirCollectionLiteral -> {
            val resolvedType = fir.resolvedType
            if (resolvedType is ConeErrorType) {
                object : FirDiagnosticHolder {
                    override val source: KtSourceElement? get() = fir.source
                    override val diagnostic: ConeDiagnostic get() = resolvedType.diagnostic
                    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {}
                    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement = this
                }
            } else {
                null
            }
        }

        else -> null
    }

    private val stringPlusSymbol by lazy(LazyThreadSafetyMode.PUBLICATION) {
        findStringPlusSymbol(analysisSession.firSession)
    }

    private fun FirElement.toKaResolutionAttempt(
        psi: KtElement,
        resolveCalleeExpressionOfFunctionCall: Boolean,
        resolveFragmentOfCall: Boolean,
    ): KaCallResolutionAttempt? {
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
            return KaBaseCallResolutionSuccess(
                backingCall = KaBaseSimpleFunctionCall(
                    backingPartiallyAppliedSymbol = KaBasePartiallyAppliedSymbol(
                        backingSignature = signature,
                        dispatchReceiver = KaBaseExplicitReceiverValue(
                            expression = leftArg,
                            backingType = analysisSession.builtinTypes.string,
                            isSafeNavigation = false,
                        ),
                        extensionReceiver = null,
                        contextArguments = emptyList(),
                    ),
                    backingArgumentMapping = mapOf(rightArg to signature.valueParameters.first()),
                    backingTypeArgumentsMapping = emptyMap(),
                )
            )
        }

        if (this is FirResolvedQualifier) {
            val callExpression = (psi as? KtExpression)?.getPossiblyQualifiedCallExpression()
            if (callExpression != null) {
                val constructors = findQualifierConstructors()
                val calls = toKaCalls(constructors)
                return KaBaseCallResolutionError(
                    backedDiagnostic = inapplicableCandidateDiagnostic(),
                    backingCandidateCalls = calls,
                )
            }
        }

        if (this is FirImplicitInvokeCall) {

            // If we have a PSI expression like `Foo.Bar.Baz()` and try to resolve `Bar` part,
            // and the only FIR that we have for that PSI is an implicit invoke call, that means that
            // `Foo.Bar` is definitely not a property access - otherwise it would have had its own FIR.
            // So, it does not make sense to try to resolve such parts of qualifiers as KaCallResolutionSuccess
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
                return explicitReceiver?.toKaResolutionAttempt(
                    psiToResolve,
                    resolveCalleeExpressionOfFunctionCall = false,
                    resolveFragmentOfCall = resolveFragmentOfCall
                )
            }
        }

        fun <T> transformErrorReference(
            call: FirElement,
            calleeReference: T,
        ): KaCallResolutionAttempt where T : FirNamedReference, T : FirDiagnosticHolder {
            return transformErrorReference(psi, call, calleeReference, resolveFragmentOfCall)
        }

        return when (this) {
            // FIR does not resolve to a symbol for equality calls.
            is FirEqualityOperatorCall -> toKaResolutionAttempt(psi)
            is FirResolvable, is FirVariableAssignment -> {
                when (val calleeReference = toReference(analysisSession.firSession)) {
                    is FirResolvedErrorReference -> transformErrorReference(this, calleeReference)
                    is FirResolvedNamedReference -> when (calleeReference.resolvedSymbol) {
                        // `calleeReference.resolvedSymbol` isn't guaranteed to be callable. For example, function type parameters used in
                        // expression positions (e.g. `T` in `println(T)`) are parsed as `KtSimpleNameExpression` and built into
                        // `FirPropertyAccessExpression` (which is `FirResolvable`).
                        is FirCallableSymbol<*> -> createKaCall(
                            psi = psi,
                            fir = this,
                            calleeReference = calleeReference,
                            candidate = null,
                            resolveFragmentOfCall = resolveFragmentOfCall,
                        )?.let(::KaBaseCallResolutionSuccess)

                        else -> null
                    }

                    is FirErrorNamedReference -> transformErrorReference(this, calleeReference)
                    // Unresolved delegated constructor call is untransformed and end up as an `FirSuperReference`
                    is FirSuperReference -> {
                        val delegatedConstructorCall = this as? FirDelegatedConstructorCall ?: return null
                        val errorTypeRef = delegatedConstructorCall.constructedTypeRef as? FirErrorTypeRef ?: return null
                        val psiSource = psi.toKtPsiSourceElement()
                        val kaDiagnostic = errorTypeRef.diagnostic.asKaDiagnostic(source ?: psiSource, psiSource) ?: return null
                        KaBaseCallResolutionError(
                            backedDiagnostic = kaDiagnostic,
                            backingCandidateCalls = emptyList(),
                        )
                    }

                    else -> null
                }
            }

            is FirCollectionLiteral -> toKaResolutionAttempt()
            is FirComparisonExpression -> compareToCall.toKaResolutionAttempt(
                psi,
                resolveCalleeExpressionOfFunctionCall,
                resolveFragmentOfCall
            )

            is FirSafeCallExpression -> selector.toKaResolutionAttempt(
                psi,
                resolveCalleeExpressionOfFunctionCall,
                resolveFragmentOfCall
            )

            is FirSmartCastExpression -> originalExpression.toKaResolutionAttempt(
                psi, resolveCalleeExpressionOfFunctionCall, resolveFragmentOfCall
            )

            is FirWhileLoop if psi is KtForExpression -> resolveForLoopCall(this)

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

    private fun createKaCall(
        psi: KtElement,
        fir: FirResolvable,
        candidate: Candidate?,
        resolveFragmentOfCall: Boolean,
    ): KaSingleOrMultiCall? = createKaCall(
        psi = psi,
        fir = fir,
        calleeReference = fir.calleeReference,
        candidate = candidate,
        resolveFragmentOfCall = resolveFragmentOfCall,
    )

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

    private fun createKaCall(
        psi: KtElement,
        fir: FirElement,
        calleeReference: FirReference,
        candidate: Candidate?,
        resolveFragmentOfCall: Boolean,
    ): KaSingleOrMultiCall? {
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
            val substitutor = substitutorByMap(firTypeArgumentsMapping, firSession).toKaSubstitutor()

            // This is crucial to create a signature by Fir symbol as it can be call-site substitution
            val unsubstitutedSignature = targetSymbol.toKaSignature()
            unsubstitutedSignature.substitute(substitutor)
        }

        var firstArgIsExtensionReceiver = false
        var isImplicitInvoke = false

        fun buildFunctionCall(
            partiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaFunctionSymbol>,
            argumentMapping: Map<KtExpression, KaVariableSignature<KaParameterSymbol>>,
            typeArgumentsMapping: Map<KaTypeParameterSymbol, KaType>,
        ): KaFunctionCall<*> = if (isImplicitInvoke) {
            val functionSymbol = partiallyAppliedSymbol.symbol
            requireWithAttachment(
                functionSymbol is KaNamedFunctionSymbol,
                { "Expected ${KaNamedFunctionSymbol::class.simpleName}, but got ${functionSymbol::class.simpleName}" },
            ) {
                withSymbolAttachment("function", analysisSession, functionSymbol)
            }

            @Suppress("UNCHECKED_CAST")
            KaBaseImplicitInvokeCall(
                backingPartiallyAppliedSymbol = partiallyAppliedSymbol as KaPartiallyAppliedFunctionSymbol<KaNamedFunctionSymbol>,
                backingArgumentMapping = argumentMapping,
                backingTypeArgumentsMapping = typeArgumentsMapping,
            )
        } else {
            KaBaseSimpleFunctionCall(
                backingPartiallyAppliedSymbol = partiallyAppliedSymbol,
                backingArgumentMapping = argumentMapping,
                backingTypeArgumentsMapping = typeArgumentsMapping,
            )
        }

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
                is KtQualifiedExpression -> psi.selectorExpression
                    ?: errorWithAttachment("missing selectorExpression in PSI ${psi::class.simpleName} for FirImplicitInvokeCall") {
                        withPsiEntry("psi", psi, analysisSession::getModule)
                    }

                is KtExpression -> psi
                else -> errorWithAttachment("unexpected PSI ${psi::class.simpleName} for FirImplicitInvokeCall") {
                    withPsiEntry("psi", psi, analysisSession::getModule)
                }
            }

            if (explicitReceiverPsi is KtCallExpression) {
                explicitReceiverPsi = explicitReceiverPsi.calleeExpression
                    ?: errorWithAttachment("missing calleeExpression in PSI ${psi::class.simpleName} for FirImplicitInvokeCall") {
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
                        { "Dispatch receiver must be not null if explicitReceiverKind is DISPATCH_RECEIVER" },
                    ) {
                        withPsiEntry("explicitReceiverPsi", explicitReceiverPsi)
                        extensionReceiver?.let { withFirEntry("extensionReceiver", it) }
                        withFirSymbolEntry("target", targetSymbol)
                    }

                    dispatchReceiverValue = KaBaseExplicitReceiverValue(
                        expression = explicitReceiverPsi,
                        backingType = dispatchReceiver.resolvedType.asKaType(),
                        isSafeNavigation = false,
                    )

                    extensionReceiverValue = if (firstArgIsExtensionReceiver) {
                        when (fir) {
                            is FirFunctionCall -> fir.arguments.firstOrNull()?.toKaReceiverValue()
                            is FirPropertyAccessExpression -> fir.explicitReceiver?.toKaReceiverValue()
                            else -> null
                        }
                    } else {
                        extensionReceiver?.toKaReceiverValue()
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

                    dispatchReceiverValue = dispatchReceiver?.toKaReceiverValue()
                    extensionReceiverValue = KaBaseExplicitReceiverValue(
                        expression = explicitReceiverPsi,
                        backingType = extensionReceiver.resolvedType.asKaType(),
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
                    dispatchReceiver = candidate.dispatchReceiver?.expression?.toKaReceiverValue(),
                    extensionReceiver = candidate.chosenExtensionReceiver?.expression?.toKaReceiverValue(),
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
                dispatchReceiver = fir.dispatchReceiver?.toKaReceiverValue(),
                extensionReceiver = fir.extensionReceiver?.toKaReceiverValue(),
                contextArguments = fir.contextArguments.toKaContextParameterValues(),
            )

            fir is FirVariableAssignment -> KaBasePartiallyAppliedSymbol(
                backingSignature = signature,
                dispatchReceiver = fir.dispatchReceiver?.toKaReceiverValue(),
                extensionReceiver = fir.extensionReceiver?.toKaReceiverValue(),
                contextArguments = fir.contextArguments.toKaContextParameterValues(),
            )

            fir is FirDelegatedConstructorCall -> KaBasePartiallyAppliedSymbol(
                backingSignature = signature,
                dispatchReceiver = fir.dispatchReceiver?.toKaReceiverValue(),
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
                    backingPartiallyAppliedSymbol = partiallyAppliedSymbol as KaPartiallyAppliedFunctionSymbol<KaConstructorSymbol>,
                    backingArgumentMapping = fir.createArgumentMapping(signatureOfCallee = partiallyAppliedSymbol.signature as KaFunctionSignature<*>),
                )
            }

            is FirDelegatedConstructorCall -> {
                if (partiallyAppliedSymbol.symbol !is KaConstructorSymbol) return null
                @Suppress("UNCHECKED_CAST") // safe because of the above check on targetKtSymbol
                KaBaseDelegatedConstructorCall(
                    backingPartiallyAppliedSymbol = partiallyAppliedSymbol as KaPartiallyAppliedFunctionSymbol<KaConstructorSymbol>,
                    backingKind = if (fir.isThis) KaDelegatedConstructorCall.Kind.THIS_CALL else KaDelegatedConstructorCall.Kind.SUPER_CALL,
                    backingArgumentMapping = fir.createArgumentMapping(signatureOfCallee = partiallyAppliedSymbol.signature as KaFunctionSignature<*>),
                    backingTypeArgumentsMapping = typeArgumentsMapping,
                )
            }

            is FirVariableAssignment -> {
                if (partiallyAppliedSymbol.symbol !is KaVariableSymbol) return null
                val rhs = fir.rValue.psi as? KtExpression
                @Suppress("UNCHECKED_CAST") // safe because of the above check on targetKtSymbol
                KaBaseSimpleVariableAccessCall(
                    backingPartiallyAppliedSymbol = partiallyAppliedSymbol as KaPartiallyAppliedVariableSymbol<KaVariableSymbol>,
                    backingTypeArgumentsMapping = typeArgumentsMapping,
                    backingKind = KaBaseVariableWriteAccess(value = rhs),
                    backingIsContextSensitive = calleeReference.isContextSensitive,
                )
            }

            is FirPropertyAccessExpression, is FirCallableReferenceAccess -> when (partiallyAppliedSymbol.symbol) {
                is KaVariableSymbol -> {
                    @Suppress("UNCHECKED_CAST") // safe because of the above check on targetKtSymbol
                    KaBaseSimpleVariableAccessCall(
                        backingPartiallyAppliedSymbol = partiallyAppliedSymbol as KaPartiallyAppliedVariableSymbol<KaVariableSymbol>,
                        backingTypeArgumentsMapping = typeArgumentsMapping,
                        backingKind = KaBaseVariableReadAccess,
                        backingIsContextSensitive = calleeReference.isContextSensitive,
                    )
                }

                // if errorsness call without ()
                is KaFunctionSymbol -> {
                    @Suppress("UNCHECKED_CAST") // safe because of the above check on targetKtSymbol
                    buildFunctionCall(
                        partiallyAppliedSymbol = partiallyAppliedSymbol as KaPartiallyAppliedFunctionSymbol<KaFunctionSymbol>,
                        argumentMapping = emptyMap(),
                        typeArgumentsMapping = typeArgumentsMapping,
                    )
                }
            }

            is FirFunctionCall -> {
                if (partiallyAppliedSymbol.symbol !is KaFunctionSymbol) return null
                val argumentMapping = if (candidate is Candidate) {
                    runIf(candidate.argumentMappingInitialized) { candidate.argumentMapping.unwrapAtoms() }
                } else {
                    fir.resolvedArgumentMappingIncludingContextArguments
                }

                val argumentMappingWithoutExtensionReceiver =
                    if (firstArgIsExtensionReceiver) {
                        argumentMapping?.entries?.drop(1)
                    } else {
                        argumentMapping?.entries
                    }

                @Suppress("UNCHECKED_CAST") // safe because of the above check on targetKtSymbol
                buildFunctionCall(
                    partiallyAppliedSymbol = partiallyAppliedSymbol as KaPartiallyAppliedFunctionSymbol<KaFunctionSymbol>,
                    argumentMapping = argumentMappingWithoutExtensionReceiver
                        ?.createArgumentMapping(partiallyAppliedSymbol.signature)
                        .orEmpty(),
                    typeArgumentsMapping = typeArgumentsMapping,
                )
            }

            is FirSmartCastExpression -> (fir.originalExpression as? FirResolvable)?.let {
                createKaCall(
                    psi = psi,
                    fir = it,
                    candidate = candidate,
                    resolveFragmentOfCall = resolveFragmentOfCall,
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
        compoundOperationProvider: (KaFunctionCall<KaNamedFunctionSymbol>) -> KaCompoundOperation,
    ): KaSingleOrMultiCall? {
        if (fir !is FirFunctionCall || fir.calleeReference.name != OperatorNameConventions.SET || accessExpression !is KtArrayAccessExpression) {
            return null
        }

        val context = contextProvider(fir, accessExpression) ?: return null
        return if (resolveFragmentOfCall) {
            context.getCall
        } else {
            KaBaseCompoundArrayAccessCall(
                backingCompoundAccess = compoundOperationProvider(context.operationCall),
                backingIndexArguments = accessExpression.indexExpressions,
                backingGetterCall = context.getCall,
                backingSetterCall = context.setCall,
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
        compoundOperationProvider: (KaFunctionCall<KaNamedFunctionSymbol>) -> KaCompoundOperation,
        rhsExpression: KtExpression?,
    ): KaSingleOrMultiCall? {
        if (fir !is FirVariableAssignment || accessExpression !is KtQualifiedExpression && accessExpression !is KtNameReferenceExpression) {
            return null
        }

        val variableSymbol = fir.toPartiallyAppliedSymbol() ?: return null
        val operationCall = getOperationCallForCompoundVariableAccess(fir, accessExpression, rhsExpression) ?: return null
        val variableAccessCall = KaBaseSimpleVariableAccessCall(
            backingPartiallyAppliedSymbol = variableSymbol,
            backingTypeArgumentsMapping = typeArgumentsMapping,
            backingKind = KaBaseVariableReadAccess,
            backingIsContextSensitive = fir.calleeReference?.isContextSensitive == true,
        )

        return if (resolveFragmentOfCall) {
            variableAccessCall
        } else {
            KaBaseCompoundVariableAccessCall(
                backingVariableCall = variableAccessCall,
                backingCompoundOperation = compoundOperationProvider(operationCall),
            )
        }
    }

    private fun createKaCallForCompoundAccessConvention(
        fir: FirElement,
        accessExpression: KtExpression?,
        rhsExpression: KtExpression?,
        resolveFragmentOfCall: Boolean,
        typeArgumentsMapping: Map<KaTypeParameterSymbol, KaType>,
        contextProvider: (FirFunctionCall, KtArrayAccessExpression) -> CompoundArrayAccessContext?,
        compoundOperationProvider: (KaFunctionCall<KaNamedFunctionSymbol>) -> KaCompoundOperation,
    ): KaSingleOrMultiCall? = createKaCallForArrayAccessConvention(
        fir = fir,
        accessExpression = accessExpression,
        resolveFragmentOfCall = resolveFragmentOfCall,
        contextProvider = contextProvider,
        compoundOperationProvider = compoundOperationProvider,
    ) ?: createKaCallForVariableAccessConvention(
        fir = fir,
        accessExpression = accessExpression,
        rhsExpression = rhsExpression,
        resolveFragmentOfCall = resolveFragmentOfCall,
        typeArgumentsMapping = typeArgumentsMapping,
        compoundOperationProvider = compoundOperationProvider,
    )

    private fun <T> transformErrorReference(
        psi: KtElement,
        call: FirElement,
        calleeReference: T,
        resolveFragmentOfCall: Boolean,
    ): KaCallResolutionAttempt where T : FirNamedReference, T : FirDiagnosticHolder {
        val diagnostic = calleeReference.diagnostic
        val kaDiagnostic = calleeReference.createKaDiagnostic(psi)

        if (diagnostic is ConeHiddenCandidateError) {
            return KaBaseCallResolutionError(
                backedDiagnostic = kaDiagnostic,
                backingCandidateCalls = emptyList(),
            )
        }

        val candidateCalls = if (diagnostic is ConeDiagnosticWithCandidates) {
            diagnostic.candidates.mapNotNull {
                if (it is Candidate) {
                    createKaCall(psi, call, calleeReference, it, resolveFragmentOfCall)
                } else {
                    null
                }
            }
        } else {
            val call = createKaCall(psi, call, calleeReference, null, resolveFragmentOfCall)
            listOfNotNull(call)
        }

        return KaBaseCallResolutionError(
            backedDiagnostic = kaDiagnostic,
            backingCandidateCalls = candidateCalls,
        )
    }

    private fun extractForLoopFirCalls(firLoop: FirWhileLoop): Triple<FirFunctionCall, FirFunctionCall, FirFunctionCall>? {
        val hasNextCall = firLoop.condition as? FirFunctionCall ?: return null

        val iteratorPropertyAccess = hasNextCall.explicitReceiver as? FirQualifiedAccessExpression ?: return null
        val iteratorPropertySymbol = (iteratorPropertyAccess.calleeReference as? FirResolvedNamedReference)
            ?.resolvedSymbol as? FirPropertySymbol ?: return null

        @OptIn(SymbolInternals::class)
        val iteratorCall = iteratorPropertySymbol.fir.initializer as? FirFunctionCall ?: return null

        @OptIn(SymbolInternals::class)
        val nextCall = (firLoop.block.statements.firstOrNull() as? FirProperty)?.initializer as? FirFunctionCall ?: return null

        return Triple(iteratorCall, hasNextCall, nextCall)
    }

    private fun resolveForLoopSymbols(firLoop: FirWhileLoop): KaSymbolResolutionAttempt? {
        val (iteratorCall, hasNextCall, nextCall) = extractForLoopFirCalls(firLoop) ?: return null
        val psi = firLoop.psi as? KtForExpression ?: return null
        val calls = listOf(iteratorCall, hasNextCall, nextCall)

        val firstError = calls.firstNotNullOfOrNull { it.calleeReference as? FirDiagnosticHolder }
        if (firstError != null) {
            return firstError.toKaSymbolResolutionError(psi)
        }

        val symbols = calls.map {
            it.calleeReference.symbol?.buildSymbol(firSymbolBuilder) ?: return null
        }

        return KaBaseSymbolResolutionSuccess(symbols, token)
    }

    private fun resolveForLoopCall(firLoop: FirWhileLoop): KaCallResolutionAttempt? {
        val (firIteratorCall, firHasNextCall, firNextCall) = extractForLoopFirCalls(firLoop) ?: return null
        val psi = firLoop.psi as? KtForExpression ?: return null
        val calls = listOf(firIteratorCall, firHasNextCall, firNextCall)

        for (firCall in calls) {
            val ref = firCall.calleeReference
            if (ref is FirDiagnosticHolder) {
                return transformErrorReference(psi, firCall, ref, resolveFragmentOfCall = false)
            }
        }

        val iteratorCall = buildForLoopFunctionCall(firIteratorCall) ?: return null
        val hasNextCall = buildForLoopFunctionCall(firHasNextCall) ?: return null
        val nextCall = buildForLoopFunctionCall(firNextCall) ?: return null

        return KaBaseCallResolutionSuccess(KaBaseForLoopCall(iteratorCall, hasNextCall, nextCall))
    }

    private fun buildForLoopFunctionCall(firFunctionCall: FirFunctionCall): KaFunctionCall<KaNamedFunctionSymbol>? {
        val functionSymbol = (firFunctionCall.calleeReference as? FirResolvedNamedReference)
            ?.resolvedSymbol as? FirNamedFunctionSymbol ?: return null

        val firTypeArgumentsMapping = firFunctionCall.toFirTypeArgumentsMapping(functionSymbol)
        val typeArgumentsMapping = firTypeArgumentsMapping.asKaTypeParametersMapping()

        val kaSignature = with(analysisSession) {
            val substitutor = substitutorByMap(firTypeArgumentsMapping, firSession).toKaSubstitutor()
            functionSymbol.toKaSignature().substitute(substitutor)
        }

        val partiallyAppliedSymbol = KaBasePartiallyAppliedSymbol(
            backingSignature = kaSignature,
            dispatchReceiver = firFunctionCall.dispatchReceiver?.toKaReceiverValue(),
            extensionReceiver = firFunctionCall.extensionReceiver?.toKaReceiverValue(),
            contextArguments = firFunctionCall.contextArguments.toKaContextParameterValues(),
        )

        @Suppress("UNCHECKED_CAST")
        return KaBaseSimpleFunctionCall(
            backingPartiallyAppliedSymbol = partiallyAppliedSymbol,
            backingArgumentMapping = emptyMap(),
            backingTypeArgumentsMapping = typeArgumentsMapping,
        ) as KaFunctionCall<KaNamedFunctionSymbol>
    }

    private fun handleCompoundAccessCall(
        psi: KtElement,
        fir: FirElement,
        resolveFragmentOfCall: Boolean,
        typeArgumentsMapping: Map<KaTypeParameterSymbol, KaType>,
    ): KaSingleOrMultiCall? {
        return when (psi) {
            is KtBinaryExpression if psi.operationToken in KtTokens.AUGMENTED_ASSIGNMENTS -> {
                val rightOperandPsi = deparenthesize(psi.right) ?: return null
                val leftOperandPsi = deparenthesize(psi.left) ?: return null
                val compoundAssignKind = psi.getCompoundAssignKind()

                createKaCallForCompoundAccessConvention(
                    fir = fir,
                    accessExpression = leftOperandPsi,
                    rhsExpression = rightOperandPsi,
                    resolveFragmentOfCall = resolveFragmentOfCall,
                    typeArgumentsMapping = typeArgumentsMapping,
                    contextProvider = { fir, arrayAccessExpression ->
                        getCompoundArrayAccessContext(
                            firCall = fir,
                            lhsArrayAccessExpression = arrayAccessExpression,
                            rhsExpression = rightOperandPsi,
                        )
                    },
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
                    rhsExpression = null,
                    resolveFragmentOfCall = resolveFragmentOfCall,
                    typeArgumentsMapping = typeArgumentsMapping,
                    contextProvider = { firCall, ktExpression ->
                        getCompoundArrayAccessContext(
                            firCall = firCall,
                            lhsArrayAccessExpression = ktExpression,
                            rhsExpression = null,
                        )
                    },
                    compoundOperationProvider = { KaBaseCompoundUnaryOperation(it, incOrDecOperationKind, precedence) },
                )
            }
            else -> null
        }
    }

    private class CompoundArrayAccessContext(
        val operationCall: KaFunctionCall<KaNamedFunctionSymbol>,
        val getCall: KaFunctionCall<KaNamedFunctionSymbol>,
        val setCall: KaFunctionCall<KaNamedFunctionSymbol>,
    )

    private fun getCompoundArrayAccessContext(
        firCall: FirFunctionCall,
        lhsArrayAccessExpression: KtArrayAccessExpression,
        rhsExpression: KtExpression?,
    ): CompoundArrayAccessContext? {
        // The last argument of `set` is the new value to be set. This value should be a call to the respective `plus`, `minus`,
        // `times`, `div`, or `rem` function.
        val firOperationCall = firCall.arguments.lastOrNull() as? FirFunctionCall ?: return null

        // The explicit receiver for both `get` and `set` call should be the array expression.
        val firExplicitReceiver = firOperationCall.explicitReceiver ?: return null
        val firGetCall = firExplicitReceiver as? FirFunctionCall // case for array access and prefix
            ?: getInitializerOfReferencedLocalVariable(firExplicitReceiver) // case for postfix
            ?: return null

        // The explicit receiver in this case is a synthetic FirFunctionCall to `get`, which does not have a corresponding PSI. So
        // we use the `lhsArrayAccessExpression` as the supplement.
        val operationPartiallyAppliedSymbol = firOperationCall.toPartiallyAppliedSymbol(lhsArrayAccessExpression) ?: return null

        // The explicit receiver for both `get` and `set` call should be the array expression.
        val arrayExpression = lhsArrayAccessExpression.arrayExpression
        val getPartiallyAppliedSymbol = firGetCall.toPartiallyAppliedSymbol(arrayExpression) ?: return null
        val setPartiallyAppliedSymbol = firCall.toPartiallyAppliedSymbol(arrayExpression) ?: return null

        val operationArgumentsMapping = listOfNotNull(lhsArrayAccessExpression, rhsExpression)
            .zip(operationPartiallyAppliedSymbol.signature.valueParameters)
            .toMap()

        val operationCall = KaBaseSimpleFunctionCall(
            backingPartiallyAppliedSymbol = operationPartiallyAppliedSymbol,
            backingArgumentMapping = operationArgumentsMapping,
            backingTypeArgumentsMapping = firCall
                .toFirTypeArgumentsMapping(symbol = operationPartiallyAppliedSymbol.symbol.firSymbol)
                .asKaTypeParametersMapping(),
        )

        val indexExpressions = lhsArrayAccessExpression.indexExpressions
        val getArgumentMapping = indexExpressions.zip(getPartiallyAppliedSymbol.signature.valueParameters).toMap()
        val getCall = KaBaseSimpleFunctionCall(
            backingPartiallyAppliedSymbol = getPartiallyAppliedSymbol,
            backingArgumentMapping = getArgumentMapping,
            backingTypeArgumentsMapping = firCall
                .toFirTypeArgumentsMapping(symbol = getPartiallyAppliedSymbol.symbol.firSymbol)
                .asKaTypeParametersMapping(),
        )

        val setArgumentsMapping = mapOf(indexExpressions.last() to setPartiallyAppliedSymbol.signature.valueParameters.last())
        val setCall = KaBaseSimpleFunctionCall(
            backingPartiallyAppliedSymbol = setPartiallyAppliedSymbol,
            backingArgumentMapping = setArgumentsMapping,
            backingTypeArgumentsMapping = firCall
                .toFirTypeArgumentsMapping(symbol = setPartiallyAppliedSymbol.symbol.firSymbol)
                .asKaTypeParametersMapping(),
        )

        @Suppress("UNCHECKED_CAST")
        return CompoundArrayAccessContext(
            operationCall = operationCall as KaFunctionCall<KaNamedFunctionSymbol>,
            getCall = getCall as KaFunctionCall<KaNamedFunctionSymbol>,
            setCall = setCall as KaFunctionCall<KaNamedFunctionSymbol>,
        )
    }

    @OptIn(SymbolInternals::class)
    private fun getInitializerOfReferencedLocalVariable(variableReference: FirExpression): FirFunctionCall? {
        return variableReference.toReference(resolutionFacade.useSiteFirSession)
            ?.toResolvedVariableSymbol()
            ?.fir
            ?.initializer as? FirFunctionCall
    }

    private fun getOperationCallForCompoundVariableAccess(
        fir: FirVariableAssignment,
        leftOperandPsi: KtExpression,
        rightOperandPsi: KtExpression?,
    ): KaFunctionCall<KaNamedFunctionSymbol>? {
        // The new value is a call to the appropriate operator function.
        val firOperationCall = fir.rValue as? FirFunctionCall ?: getInitializerOfReferencedLocalVariable(fir.rValue) ?: return null
        val operationPartiallyAppliedSymbol = firOperationCall.toPartiallyAppliedSymbol(leftOperandPsi) ?: return null

        val operationArgumentsMapping = listOfNotNull(leftOperandPsi, rightOperandPsi)
            .zip(operationPartiallyAppliedSymbol.signature.valueParameters)
            .toMap()

        val functionCall = KaBaseSimpleFunctionCall(
            backingPartiallyAppliedSymbol = operationPartiallyAppliedSymbol,
            backingArgumentMapping = operationArgumentsMapping,
            backingTypeArgumentsMapping = firOperationCall
                .toFirTypeArgumentsMapping(symbol = operationPartiallyAppliedSymbol.symbol.firSymbol)
                .asKaTypeParametersMapping(),
        )

        @Suppress("UNCHECKED_CAST")
        return functionCall as KaFunctionCall<KaNamedFunctionSymbol>
    }

    private fun FirVariableAssignment.toPartiallyAppliedSymbol(): KaPartiallyAppliedVariableSymbol<KaVariableSymbol>? {
        val variableRef = calleeReference as? FirResolvedNamedReference ?: return null
        val variableSymbol = variableRef.resolvedSymbol as? FirVariableSymbol<*> ?: return null
        val substitutor = unwrapLValue()?.createConeSubstitutorFromTypeArguments(rootModuleSession) ?: return null
        val kaSignature = variableSymbol.toKaSignature()
        return KaBasePartiallyAppliedSymbol(
            backingSignature = kaSignature.substitute(substitutor.toKaSubstitutor()),
            dispatchReceiver = dispatchReceiver?.toKaReceiverValue(),
            extensionReceiver = extensionReceiver?.toKaReceiverValue(),
            contextArguments = contextArguments.toKaContextParameterValues(),
        )
    }

    private fun FirFunctionCall.toPartiallyAppliedSymbol(
        explicitReceiverPsiSupplement: KtExpression? = null,
    ): KaPartiallyAppliedFunctionSymbol<KaNamedFunctionSymbol>? {
        val operationSymbol = (calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirNamedFunctionSymbol ?: return null
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
            explicitReceiverPsiSupplement.toExplicitReceiverValue(dispatchReceiver!!.resolvedType.asKaType())
        } else {
            dispatchReceiver?.toKaReceiverValue()
        }

        val extensionReceiverValue = if (explicitReceiverPsiSupplement != null && explicitReceiver == extensionReceiver) {
            explicitReceiverPsiSupplement.toExplicitReceiverValue(extensionReceiver!!.resolvedType.asKaType())
        } else {
            extensionReceiver?.toKaReceiverValue()
        }

        val kaSignature = operationSymbol.toKaSignature()
        return KaBasePartiallyAppliedSymbol(
            backingSignature = kaSignature.substitute(substitutor.toKaSubstitutor()),
            dispatchReceiver = dispatchReceiverValue,
            extensionReceiver = extensionReceiverValue,
            contextArguments = contextArguments.toKaContextParameterValues(),
        )
    }

    private fun List<FirExpression>.toKaContextParameterValues(): List<KaReceiverValue> = mapNotNull { expression ->
        val receiverValue = expression.toKaReceiverValue()
        if (receiverValue == null && expression !is FirErrorExpression) {
            logger<KaFirResolver>().logErrorWithAttachment("Unexpected null receiver value for context argument") {
                withFirEntry("expression", expression)
            }
        }

        receiverValue
    }

    private fun FirExpression.toKaReceiverValue(): KaReceiverValue? {
        return when (this) {
            is FirSmartCastExpression -> {
                val result = originalExpression.toKaReceiverValue()
                if (result != null && isStable) {
                    KaBaseSmartCastedReceiverValue(result, smartcastType.coneType.asKaType())
                } else {
                    result
                }
            }

            is FirThisReceiverExpression if isImplicit -> {
                val symbol = when (val firSymbol = calleeReference.boundSymbol) {
                    is FirClassSymbol<*> -> firSymbol.toKaSymbol()
                    is FirReceiverParameterSymbol -> firSymbolBuilder.callableBuilder.buildExtensionReceiverSymbol(firSymbol)
                        ?: return null

                    is FirTypeAliasSymbol, is FirTypeParameterSymbol -> errorWithFirSpecificEntries(
                        message = "Unexpected ${FirThisOwnerSymbol::class.simpleName}: ${firSymbol::class.simpleName}",
                        fir = firSymbol.fir
                    )

                    null -> return null
                }

                KaBaseImplicitReceiverValue(symbol, resolvedType.asKaType())
            }

            is FirPropertyAccessExpression if source?.kind is KtFakeSourceElementKind.ImplicitContextParameterArgument -> {
                val firSymbol = calleeReference.symbol ?: return null

                require(firSymbol is FirValueParameterSymbol) { "Unexpected symbol ${firSymbol::class.simpleName}" }
                val symbol = firSymbolBuilder.variableBuilder.buildParameterSymbol(firSymbol)
                KaBaseImplicitReceiverValue(symbol, resolvedType.asKaType())
            }

            is FirResolvedQualifier if this.source?.kind is KtFakeSourceElementKind.ImplicitReceiver -> {
                val symbol = this.symbol ?: return null
                KaBaseImplicitReceiverValue(symbol.toKaSymbol(), resolvedType.asKaType())
            }

            else -> {
                val psi = psi
                if (psi !is KtExpression) return null
                psi.toExplicitReceiverValue(resolvedType.asKaType())
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
     * case, the resulting [KaCallResolutionSuccess] would contain no type arguments at all, which can cause problems later. If too few type arguments are
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

    private fun FirCollectionLiteral.toTypeArgumentsMapping(symbol: KaDeclarationSymbol): Map<KaTypeParameterSymbol, KaType> {
        val elementType = resolvedType.arrayElementType()?.asKaType() ?: return emptyMap()
        val typeParameter = symbol.typeParameters.singleOrNull() ?: return emptyMap()
        return mapOf(typeParameter to elementType)
    }

    // TODO: Refactor common code with FirElement.toKtCallInfo() when other FirResolvables are handled
    private fun FirElement.collectCallCandidates(
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

            is FirResolvedQualifier -> toKaCallCandidates()
            is FirDelegatedConstructorCall -> collectCallCandidatesForDelegatedConstructorCall(psi, resolveFragmentOfCall)
            else -> toKaResolutionAttempt(psi, resolveCalleeExpressionOfFunctionCall, resolveFragmentOfCall).toKaCallCandidates()
        }
    }

    private fun FirResolvedQualifier.toKaCallCandidates(): List<KaCallCandidate> {
        return toKaCalls(findQualifierConstructors()).map {
            KaBaseInapplicableCallCandidate(
                backingCandidate = it,
                backingIsInBestCandidates = false,
                backingDiagnostic = inapplicableCandidateDiagnostic()
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
            firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(key) to value.asKaType()
        }.toMap()
    }

    private fun FirResolvedQualifier.toKaCalls(constructors: List<FirConstructorSymbol>): List<KaFunctionCall<*>> =
        constructors.map { constructor ->
            val signature = constructor.toKaSignature()
            val partiallyAppliedSymbol = KaBasePartiallyAppliedSymbol(
                backingSignature = signature as KaFunctionSignature<*>,
                dispatchReceiver = null,
                extensionReceiver = null,
                contextArguments = emptyList(),
            )

            val firTypeArgumentsMapping = toFirTypeArgumentsMapping(constructor)
            val typeArgumentsMapping = firTypeArgumentsMapping.asKaTypeParametersMapping()
            KaBaseSimpleFunctionCall(
                backingPartiallyAppliedSymbol = partiallyAppliedSymbol,
                backingArgumentMapping = emptyMap(),
                backingTypeArgumentsMapping = typeArgumentsMapping,
            )
        }

    private fun FirQualifiedAccessExpression.collectCallCandidates(
        psi: KtElement,
        resolveFragmentOfCall: Boolean,
    ): List<KaCallCandidate> {
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
            .getAllCandidatesForDelegatedConstructor(analysisSession.resolutionFacade, this, derivedClass.toLookupTag(), psi)

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
        is KaCallResolutionSuccess -> listOf(KaBaseApplicableCallCandidate(backingCandidate = call, backingIsInBestCandidates = true))
        is KaCallResolutionError -> candidateCalls.map {
            KaBaseInapplicableCallCandidate(
                backingCandidate = it,
                backingIsInBestCandidates = true,
                backingDiagnostic = diagnostic,
            )
        }

        null -> emptyList()
    }

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
            return KaBaseApplicableCallCandidate(
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
        val kaDiagnostic = resolvable.source?.let { diagnostic.asKaDiagnostic(it, element.toKtPsiSourceElement()) }
            ?: KaNonBoundToPsiErrorDiagnostic(factoryName = FirErrors.OTHER_ERROR.name, diagnostic.reason, token)

        return KaBaseInapplicableCallCandidate(
            backingCandidate = call,
            backingIsInBestCandidates = isInBestCandidates,
            backingDiagnostic = kaDiagnostic,
        )
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

    private val unresolvedArrayOfDiagnostic: KaDiagnostic
        get() = KaNonBoundToPsiErrorDiagnostic(
            factoryName = FirErrors.OTHER_ERROR.name,
            defaultMessage = "type of arrayOf call is not resolved",
            token = token
        )

    private fun FirCollectionLiteral.toKaResolutionAttempt(): KaCallResolutionAttempt? {
        val arrayOfSymbol = with(analysisSession) {
            val type = resolvedType as? ConeClassLikeType ?: return run {
                val defaultArrayOfSymbol = arrayOfSymbol(ArrayFqNames.ARRAY_OF_FUNCTION) ?: return null
                val substitutor = createSubstitutorFromTypeArguments(defaultArrayOfSymbol)
                val partiallyAppliedSymbol = KaBasePartiallyAppliedSymbol(
                    backingSignature = with(useSiteSession) { defaultArrayOfSymbol.substitute(substitutor) },
                    dispatchReceiver = null,
                    extensionReceiver = null,
                    contextArguments = emptyList(),
                )

                KaBaseCallResolutionError(
                    backedDiagnostic = unresolvedArrayOfDiagnostic,
                    backingCandidateCalls = listOf(
                        KaBaseSimpleFunctionCall(
                            backingPartiallyAppliedSymbol = partiallyAppliedSymbol,
                            backingArgumentMapping = createArgumentMapping(defaultArrayOfSymbol, substitutor),
                            backingTypeArgumentsMapping = toTypeArgumentsMapping(defaultArrayOfSymbol),
                        )
                    ),
                )
            }

            val factoryName = toArrayOfFactoryName(
                expectedType = type,
                session = analysisSession.firSession,
                eagerlyReturnNonPrimitive = true
            ) ?: return null

            arrayOfSymbol(factoryName)
        } ?: return null

        val substitutor = createSubstitutorFromTypeArguments(arrayOfSymbol)
        val partiallyAppliedSymbol = KaBasePartiallyAppliedSymbol(
            backingSignature = with(analysisSession) { arrayOfSymbol.substitute(substitutor) },
            dispatchReceiver = null,
            extensionReceiver = null,
            contextArguments = emptyList(),
        )

        return KaBaseCallResolutionSuccess(
            backingCall = KaBaseSimpleFunctionCall(
                backingPartiallyAppliedSymbol = partiallyAppliedSymbol,
                backingArgumentMapping = createArgumentMapping(arrayOfSymbol, substitutor),
                backingTypeArgumentsMapping = toTypeArgumentsMapping(arrayOfSymbol),
            )
        )
    }

    private fun FirCollectionLiteral.createSubstitutorFromTypeArguments(arrayOfSymbol: KaNamedFunctionSymbol): KaSubstitutor {
        val firSymbol = arrayOfSymbol.firSymbol
        // No type parameter means this is an arrayOf call of primitives, in which case there is no type arguments
        val typeParameter = firSymbol.fir.typeParameters.singleOrNull() ?: return KaSubstitutor.Empty(token)

        val elementType = resolvedType.arrayElementType() ?: return KaSubstitutor.Empty(token)
        val coneSubstitutor = substitutorByMap(mapOf(typeParameter.symbol to elementType), rootModuleSession)
        return firSymbolBuilder.typeBuilder.buildSubstitutor(coneSubstitutor)
    }

    private fun FirEqualityOperatorCall.toKaResolutionAttempt(psi: KtElement): KaCallResolutionAttempt? {
        val binaryExpression = deparenthesize(psi as? KtExpression) as? KtBinaryExpression ?: return null
        val leftPsi = binaryExpression.left ?: return null
        val rightPsi = binaryExpression.right ?: return null
        return when (operation) {
            FirOperation.EQ, FirOperation.NOT_EQ -> {
                val leftOperand = arguments.firstOrNull() ?: return null

                val equalsSymbol = getEqualsSymbol() ?: return null
                val kaSignature = equalsSymbol.toKaSignature()
                KaBaseCallResolutionSuccess(
                    backingCall = KaBaseSimpleFunctionCall(
                        backingPartiallyAppliedSymbol = KaBasePartiallyAppliedSymbol(
                            backingSignature = kaSignature,
                            dispatchReceiver = KaBaseExplicitReceiverValue(
                                expression = leftPsi,
                                backingType = leftOperand.resolvedType.asKaType(),
                                isSafeNavigation = false,
                            ),
                            extensionReceiver = null,
                            contextArguments = emptyList(),
                        ),
                        backingArgumentMapping = mapOf(rightPsi to kaSignature.valueParameters.first()),
                        backingTypeArgumentsMapping = emptyMap(),
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

    private fun FirCall.createArgumentMapping(signatureOfCallee: KaFunctionSignature<*>): Map<KtExpression, KaVariableSignature<KaParameterSymbol>> {
        return resolvedArgumentMappingIncludingContextArguments?.entries.createArgumentMapping(signatureOfCallee)
    }

    private fun Collection<MutableMap.MutableEntry<FirExpression, FirValueParameter>>?.createArgumentMapping(
        signatureOfCallee: KaFunctionSignature<*>,
    ): Map<KtExpression, KaVariableSignature<KaParameterSymbol>> {
        if (isNullOrEmpty()) return emptyMap()

        val paramSignatureByName = buildMap {
            fun associate(parameters: List<KaVariableSignature<KaParameterSymbol>>) = parameters.forEach { parameter ->
                // We intentionally use `symbol.name` instead of `name` here, since
                // the '@ParameterName' does not affect 'FirValueParameter.name'
                put(parameter.symbol.name, parameter)
            }

            associate(signatureOfCallee.valueParameters)
            associate(signatureOfCallee.contextParameters)
        }

        val argumentMapping = LinkedHashMap<KtExpression, KaVariableSignature<KaParameterSymbol>>(size)
        this.forEach { (firExpression, firValueParameter) ->
            val parameterSymbol = paramSignatureByName[firValueParameter.name] ?: return@forEach
            mapArgumentExpressionToParameter(firExpression, parameterSymbol, argumentMapping)
        }

        return argumentMapping
    }

    private fun FirCollectionLiteral.createArgumentMapping(
        arrayOfSymbol: KaNamedFunctionSymbol,
        substitutor: KaSubstitutor,
    ): Map<KtExpression, KaVariableSignature<KaParameterSymbol>> {
        val arguments = argumentList.arguments
        if (arguments.isEmpty()) return emptyMap()

        val argumentMapping = LinkedHashMap<KtExpression, KaVariableSignature<KaParameterSymbol>>(arguments.size)
        val parameterSymbol = arrayOfSymbol.valueParameters.single()

        for (firExpression in arguments) {
            mapArgumentExpressionToParameter(
                firExpression,
                with(analysisSession) { parameterSymbol.substitute(substitutor) },
                argumentMapping,
            )
        }

        return argumentMapping
    }

    private fun mapArgumentExpressionToParameter(
        argumentExpression: FirExpression,
        parameterSymbol: KaVariableSignature<KaParameterSymbol>,
        argumentMapping: LinkedHashMap<KtExpression, KaVariableSignature<KaParameterSymbol>>,
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
        }?.topParenthesizedParentOrMe()
    }

    private inline fun <R> wrapError(element: KtElement, action: () -> R): R = try {
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

    private fun createKaDiagnostic(
        source: KtSourceElement?,
        coneDiagnostic: ConeDiagnostic,
        psi: KtElement?,
    ): KaDiagnostic = source?.let { coneDiagnostic.asKaDiagnostic(it, psi?.toKtPsiSourceElement()) }
        ?: KaNonBoundToPsiErrorDiagnostic(factoryName = FirErrors.OTHER_ERROR.name, coneDiagnostic.reason, token)

    private fun FirDiagnosticHolder.createKaDiagnostic(psi: KtElement?): KaDiagnostic = createKaDiagnostic(source, diagnostic, psi)
}

private val FirReference.isContextSensitive: Boolean
    get() = this is FirResolvedNamedReference && resolvedSymbolOrigin == FirResolvedSymbolOrigin.ContextSensitive
