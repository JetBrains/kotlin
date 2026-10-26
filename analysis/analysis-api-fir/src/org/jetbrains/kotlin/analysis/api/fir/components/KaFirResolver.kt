/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.components.resolver.*
import org.jetbrains.kotlin.analysis.api.fir.resolution.*
import org.jetbrains.kotlin.analysis.api.fir.utils.KaNonBoundToPsiErrorDiagnostic
import org.jetbrains.kotlin.analysis.api.impl.base.util.withPsiEntry
import org.jetbrains.kotlin.analysis.api.internals.KaInternalsResolver
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.projectStructure.kaModule
import org.jetbrains.kotlin.analysis.api.resolution.*
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbolOfTypeSafe
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolver.AllCandidatesResolver
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.findStringPlusSymbol
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnosticHolder
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCall
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate
import org.jetbrains.kotlin.fir.resolve.createConeDiagnosticForCandidateWithError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeHiddenCandidateError
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.KtPsiUtil.deparenthesize
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.resolution.KtResolvable
import org.jetbrains.kotlin.resolution.KtResolvableCall
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.util.OperatorNameConventions.EQUALS
import org.jetbrains.kotlin.utils.exceptions.checkWithAttachment
import org.jetbrains.kotlin.utils.exceptions.rethrowExceptionWithDetails
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

internal class KaFirResolver(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaFirSessionComponent(), KaInternalsResolver {
    override fun tryResolveSymbols(resolvable: KtResolvable): KaSymbolResolutionAttempt? = withValidityAssertion {
        when (resolvable) {
            is KtOperationReferenceExpression -> resolvable.tryResolveSymbolsForOperationReference()
            is KtResolvableCall -> resolvable.tryResolveSymbolsForResolvableCall()
            else -> resolvable.tryResolveSymbolsForElement()
        }
    }

    override fun resolveToSymbols(reference: KtReference): Collection<KaSymbol> = withPsiValidityAssertion(reference.element) {
        with(reference as? KaResolvableReferenceBridge) {
            if (this != null) {
                analysisSession.resolveToSymbols()
            } else {
                emptyList()
            }
        }
    }

    /**
     * Technically, symbol resolution can be more efficient than calls,
     * because calls require collecting more information (e.g., argument mappings).
     * However, the tradeoff is almost complete code duplication and duplicate caches that seem too high.
     * In reality, the reuse of call resolution is actually a benefit because its result is cached and
     * effectively reused at all entry points into the resolver API
     */
    private fun KtResolvableCall.tryResolveSymbolsForResolvableCall(): KaSymbolResolutionAttempt? = when (this) {
        // Both reference kinds may stand in either type or call positions, and in some cases the symbol-based
        // result is more specific (e.g., it prefers classes to constructors).
        // For enum entry super-type references this also means the enclosing enum class is returned instead of
        // the synthetic constructor of the call form.
        is KtNameReferenceExpression, is KtEnumEntrySuperclassReferenceExpression -> tryResolveSymbolsForElement()
        else -> null
    } ?: when (val callAttempt = tryResolveCall(this)) {
        is KaSimpleCallResolutionAttempt -> callAttempt.toSimpleSymbolResolutionAttempt()
        is KaMultiCallResolutionAttempt -> callAttempt.toSymbolResolutionAttempt()
        null -> null
    }

    /**
     * Logic for operations might be non-trivial, so it is more efficient to rely on the call resolution
     *
     * @see tryResolveSymbolsForResolvableCall
     */
    private fun KtOperationReferenceExpression.tryResolveSymbolsForOperationReference(): KaSymbolResolutionAttempt? {
        return when (val callAttempt = tryResolveCall(this)) {
            is KaSimpleCallResolutionError -> callAttempt.toSimpleSymbolResolutionAttempt()

            // Single variable access is not expected to be a result of the symbol resolve (the assignment use case)
            is KaSimpleCallResolutionSuccess if callAttempt.call !is KaVariableAccessCall -> callAttempt.toSimpleSymbolResolutionAttempt()
            is KaMultiCallResolutionAttempt -> when (callAttempt) {
                is KaCompoundArrayAccessCallResolutionAttempt -> mergeSymbolAttempts(
                    listOf(
                        callAttempt.operationCallAttempt.toSimpleSymbolResolutionAttempt(),
                        callAttempt.setterCallAttempt.toSimpleSymbolResolutionAttempt(),
                    )
                )

                is KaCompoundVariableAccessCallResolutionAttempt -> callAttempt.operationCallAttempt.toSimpleSymbolResolutionAttempt()
                else -> callAttempt.toSymbolResolutionAttempt()
            }

            else -> null
        }
    }

    private fun KtResolvable.tryResolveSymbolsForElement(): KaSymbolResolutionAttempt? {
        checkValidity()
        return performSymbolResolution(this)
    }

    override fun resolveSuccessfulSymbols(resolvable: KtResolvable): Collection<KaSymbol> = withValidityAssertion {
        tryResolveSymbols(resolvable)?.successfulSymbols ?: emptyList()
    }

    override fun resolveSuccessfulSymbol(resolvable: KtResolvable): KaSymbol? = withValidityAssertion {
        resolveSuccessfulSymbols(resolvable).singleOrNull()
    }

    private inline fun <reified R : KaSymbol> KtResolvable.resolveSymbolSafe(): R? = resolveSuccessfulSymbol(this) as? R

    override fun resolveSuccessfulSymbol(annotationEntry: KtAnnotationEntry): KaConstructorSymbol? = annotationEntry.resolveSymbolSafe()
    override fun resolveSuccessfulSymbol(superTypeCallEntry: KtSuperTypeCallEntry): KaConstructorSymbol? =
        superTypeCallEntry.resolveSymbolSafe()

    override fun resolveSuccessfulSymbol(constructorDelegationCall: KtConstructorDelegationCall): KaConstructorSymbol? =
        constructorDelegationCall.resolveSymbolSafe()

    override fun resolveSuccessfulSymbol(constructorDelegationReferenceExpression: KtConstructorDelegationReferenceExpression): KaConstructorSymbol? =
        constructorDelegationReferenceExpression.resolveSymbolSafe()

    override fun resolveSuccessfulSymbol(callElement: KtCallElement): KaFunctionSymbol? = callElement.resolveSymbolSafe()
    override fun resolveSuccessfulSymbol(callableReferenceExpression: KtCallableReferenceExpression): KaCallableSymbol? =
        callableReferenceExpression.resolveSymbolSafe()

    override fun resolveSuccessfulSymbol(arrayAccessExpression: KtArrayAccessExpression): KaNamedFunctionSymbol? =
        arrayAccessExpression.resolveSymbolSafe()

    override fun resolveSuccessfulSymbol(collectionLiteralExpression: KtCollectionLiteralExpression): KaNamedFunctionSymbol? =
        collectionLiteralExpression.resolveSymbolSafe()

    override fun resolveSuccessfulSymbol(enumEntrySuperclassReferenceExpression: KtEnumEntrySuperclassReferenceExpression): KaNamedClassSymbol? =
        enumEntrySuperclassReferenceExpression.resolveSymbolSafe()

    override fun resolveSuccessfulSymbol(labelReferenceExpression: KtLabelReferenceExpression): KaDeclarationSymbol? =
        labelReferenceExpression.resolveSymbolSafe()

    override fun resolveSuccessfulSymbol(returnExpression: KtReturnExpression): KaFunctionSymbol? = returnExpression.resolveSymbolSafe()
    override fun resolveSuccessfulSymbol(whenConditionInRange: KtWhenConditionInRange): KaNamedFunctionSymbol? =
        whenConditionInRange.resolveSymbolSafe()

    override fun resolveSuccessfulSymbol(destructuringDeclarationEntry: KtDestructuringDeclarationEntry): KaCallableSymbol? =
        destructuringDeclarationEntry.resolveSymbolSafe()

    override fun resolveSuccessfulSymbol(qualifiedExpression: KtQualifiedExpression): KaCallableSymbol? =
        qualifiedExpression.resolveSymbolSafe()

    override fun resolveSuccessfulSymbol(constructorCalleeExpression: KtConstructorCalleeExpression): KaConstructorSymbol? =
        constructorCalleeExpression.resolveSymbolSafe()

    override fun resolveSuccessfulSymbol(instanceExpressionWithLabel: KtInstanceExpressionWithLabel): KaDeclarationSymbol? =
        instanceExpressionWithLabel.resolveSymbolSafe()

    override fun resolveSuccessfulSymbol(nullableType: KtNullableType): KaClassifierSymbol? = nullableType.resolveSymbolSafe()
    override fun resolveSuccessfulSymbol(functionType: KtFunctionType): KaClassSymbol? = functionType.resolveSymbolSafe()
    override fun resolveSuccessfulSymbol(typeReference: KtTypeReference): KaClassifierSymbol? = typeReference.resolveSymbolSafe()
    override fun resolveSuccessfulSymbol(classLiteralExpression: KtClassLiteralExpression): KaClassifierSymbol? =
        classLiteralExpression.resolveSymbolSafe()

    override fun resolveSuccessfulSymbol(superTypeEntry: KtSuperTypeEntry): KaClassifierSymbol? = superTypeEntry.resolveSymbolSafe()
    override fun resolveSuccessfulSymbol(delegatedSuperTypeEntry: KtDelegatedSuperTypeEntry): KaClassifierSymbol? =
        delegatedSuperTypeEntry.resolveSymbolSafe()

    override fun resolveToSymbol(reference: KtReference): KaSymbol? = withPsiValidityAssertion(reference.element) {
        return resolveToSymbols(reference).singleOrNull()
    }

    private fun KtElement.tryResolveCallImpl(): KaCallResolutionAttempt? {
        val unwrappedElement = unwrapResolvableCall()
        return unwrappedElement?.let(::performCallResolution)
    }

    override fun tryResolveCall(resolvableCall: KtResolvableCall): KaCallResolutionAttempt? = withValidityAssertion {
        resolvableCall.checkValidity()
        resolvableCall.tryResolveCallImpl()
    }

    override fun resolveSuccessfulCall(resolvableCall: KtResolvableCall): KaSimpleOrMultiCall? = tryResolveCall(resolvableCall)?.successful

    private inline fun <reified R : KaSimpleOrMultiCall> KtResolvableCall.resolveCallSafe(): R? = resolveSuccessfulCall(this) as? R

    private inline fun <reified S : KaCallableSymbol, C : KaCallableSignature<S>, reified R : KaSimpleCall<S, C>> KtResolvableCall.resolveSimpleCallSafe(): R? {
        val call = resolveSuccessfulCall(this) ?: return null
        checkWithAttachment(
            call is KaSimpleCall<*, *>,
            { "Expected call of type ${KaSimpleCall::class.simpleName}, got ${call::class.simpleName}" },
        ) {
            withPsiEntry("psi", this@resolveSimpleCallSafe)
        }

        val callableSymbol = call.symbol
        checkWithAttachment(
            callableSymbol is S,
            { "Expected symbol of type ${S::class.simpleName}, got ${callableSymbol::class.simpleName}" },
        ) {
            withEntry("symbol", callableSymbol) {
                KaDebugRenderer(renderExtra = true).render(analysisSession, callableSymbol)
            }

            withPsiEntry("psi", this@resolveSimpleCallSafe)
        }

        checkWithAttachment(
            call is R,
            { "Expected call of type ${R::class.simpleName}, got ${call::class.simpleName}" }
        ) {
            withEntry("symbol", callableSymbol) {
                KaDebugRenderer(renderExtra = true).render(analysisSession, callableSymbol)
            }

            withPsiEntry("psi", this@resolveSimpleCallSafe)
        }

        return call
    }

    override fun resolveSuccessfulCall(annotationEntry: KtAnnotationEntry): KaAnnotationCall? = annotationEntry.resolveSimpleCallSafe()
    override fun resolveSuccessfulCall(superTypeCallEntry: KtSuperTypeCallEntry): KaFunctionCall<KaConstructorSymbol>? =
        superTypeCallEntry.resolveSimpleCallSafe()

    override fun resolveSuccessfulCall(constructorDelegationCall: KtConstructorDelegationCall): KaDelegatedConstructorCall? =
        constructorDelegationCall.resolveSimpleCallSafe()

    override fun resolveSuccessfulCall(constructorDelegationReferenceExpression: KtConstructorDelegationReferenceExpression): KaDelegatedConstructorCall? =
        constructorDelegationReferenceExpression.resolveSimpleCallSafe()

    override fun resolveSuccessfulCall(callElement: KtCallElement): KaFunctionCall<*>? = callElement.resolveCallSafe()
    override fun resolveSuccessfulCall(callableReferenceExpression: KtCallableReferenceExpression): KaCallableReferenceCall<*, *>? =
        callableReferenceExpression.resolveCallSafe()

    override fun resolveSuccessfulCall(arrayAccessExpression: KtArrayAccessExpression): KaFunctionCall<KaNamedFunctionSymbol>? =
        arrayAccessExpression.resolveSimpleCallSafe()

    override fun resolveSuccessfulCall(collectionLiteralExpression: KtCollectionLiteralExpression): KaFunctionCall<KaNamedFunctionSymbol>? =
        collectionLiteralExpression.resolveSimpleCallSafe()

    override fun resolveSuccessfulCall(enumEntrySuperclassReferenceExpression: KtEnumEntrySuperclassReferenceExpression): KaDelegatedConstructorCall? =
        enumEntrySuperclassReferenceExpression.resolveSimpleCallSafe()

    override fun resolveSuccessfulCall(whenConditionInRange: KtWhenConditionInRange): KaFunctionCall<KaNamedFunctionSymbol>? =
        whenConditionInRange.resolveSimpleCallSafe()

    override fun resolveSuccessfulCall(destructuringDeclarationEntry: KtDestructuringDeclarationEntry): KaSimpleCall<*, *>? =
        destructuringDeclarationEntry.resolveCallSafe()

    override fun resolveSuccessfulCall(qualifiedExpression: KtQualifiedExpression): KaSimpleCall<*, *>? = qualifiedExpression.resolveCallSafe()
    override fun resolveSuccessfulCall(forExpression: KtForExpression): KaForLoopCall? = forExpression.resolveCallSafe()
    override fun resolveSuccessfulCall(propertyDelegate: KtPropertyDelegate): KaDelegatedPropertyCall? = propertyDelegate.resolveCallSafe()

    override fun tryResolveCall(forExpression: KtForExpression): KaForLoopCallResolutionAttempt? =
        forExpression.tryResolveCallImpl() as? KaForLoopCallResolutionAttempt

    override fun tryResolveCall(propertyDelegate: KtPropertyDelegate): KaDelegatedPropertyCallResolutionAttempt? =
        propertyDelegate.tryResolveCallImpl() as? KaDelegatedPropertyCallResolutionAttempt

    override fun resolveSuccessfulCall(constructorCalleeExpression: KtConstructorCalleeExpression): KaFunctionCall<KaConstructorSymbol>? =
        constructorCalleeExpression.resolveSimpleCallSafe()

    override fun resolveSuccessfulCall(nameReferenceExpression: KtNameReferenceExpression): KaSimpleCall<*, *>? =
        nameReferenceExpression.resolveCallSafe()

    override fun resolveToCall(element: KtElement): KaCallInfo? = element.withPsiValidityAssertion {
        when (val attempt = element.tryResolveCallImpl()) {
            is KaSimpleCallResolutionError -> KaFirErrorCallInfo(attempt.candidateCalls.map { it.asKaCall() }, attempt.diagnostic)
            is KaSimpleCallResolutionSuccess -> KaFirSuccessCallInfo(attempt.call.asKaCall())
            is KaMultiCallResolutionAttempt -> attempt.toCallInfo()
            null -> null
        }
    }

    private fun KtElement.collectCallCandidatesImpl(): List<KaCallCandidate> {
        val unwrappedElement = unwrapResolvableCall()
        return unwrappedElement?.let(::performCallCandidatesCollection).orEmpty()
    }

    override fun collectCallCandidates(resolvableCall: KtResolvableCall): List<KaCallCandidate> = withValidityAssertion {
        resolvableCall.checkValidity()
        resolvableCall.collectCallCandidatesImpl()
    }

    override fun resolveToCallCandidates(element: KtElement): List<KaCallCandidateInfo> = element.withPsiValidityAssertion {
        element.collectCallCandidatesImpl().map(KaCallCandidate::asKaCallCandidateInfo)
    }

    private fun canBeResolvedAsCall(ktElement: KtElement): Boolean = when (ktElement) {
        is KtBinaryExpression -> ktElement.operationToken !in nonCallBinaryOperator
        is KtPrefixExpression -> true
        is KtPostfixExpression -> ktElement.operationToken != KtTokens.EXCLEXCL
        is KtCallElement -> true
        is KtConstructorCalleeExpression -> true
        is KtQualifiedExpression -> true
        is KtNameReferenceExpression -> ktElement.parent !is KtInstanceExpressionWithLabel
        is KtArrayAccessExpression -> true
        is KtCallableReferenceExpression -> true
        is KtWhenConditionInRange -> true
        is KtCollectionLiteralExpression -> true
        is KtConstructorDelegationReferenceExpression -> true
        is KtEnumEntrySuperclassReferenceExpression -> true
        is KtDestructuringDeclarationEntry -> true
        is KtForExpression -> true
        is KtPropertyDelegate -> true
        else -> false
    }

    private fun KtElement.unwrapResolvableCall(): KtElement? = when (this) {
        is KtOperationReferenceExpression -> parent as? KtElement
        else -> this
    }?.takeIf(::canBeResolvedAsCall)

    private fun KaMultiCallResolutionAttempt.toSymbolResolutionAttempt(): KaSymbolResolutionAttempt =
        mergeSymbolAttempts(simpleAttempts.map { it.toSimpleSymbolResolutionAttempt() })

    private fun KaSimpleCallResolutionAttempt.toSimpleSymbolResolutionAttempt(): KaSimpleSymbolResolutionAttempt = when (this) {
        is KaSimpleCallResolutionSuccess -> KaFirSimpleSymbolResolutionSuccess(backingSymbol = call.symbol)
        is KaSimpleCallResolutionError -> KaFirSimpleSymbolResolutionError(
            backingDiagnostic = diagnostic,
            backingCandidateSymbols = candidateCalls.map { it.symbol },
        )
    }

    /**
     * Merges individual symbol resolution attempts into a single result, satisfying the
     * [KaCompoundSymbolResolutionError] contract: at most one [KaSimpleSymbolResolutionSuccess]
     * (combining all successful symbols) and at least one [KaSimpleSymbolResolutionError].
     */
    private fun mergeSymbolAttempts(symbolAttempts: List<KaSimpleSymbolResolutionAttempt>): KaSymbolResolutionAttempt {
        val successSymbols = mutableListOf<KaSymbol>()
        val errors = mutableListOf<KaSimpleSymbolResolutionError>()

        for (attempt in symbolAttempts) when (attempt) {
            is KaSimpleSymbolResolutionSuccess -> successSymbols.addAll(attempt.symbols)
            is KaSimpleSymbolResolutionError -> errors.add(attempt)
        }

        if (errors.isEmpty()) {
            return KaFirSimpleSymbolResolutionSuccess(successSymbols)
        }

        if (symbolAttempts.size == 1) {
            return errors.single()
        }

        val merged = buildList {
            if (successSymbols.isNotEmpty()) {
                add(KaFirSimpleSymbolResolutionSuccess(successSymbols))
            }

            addAll(errors)
        }

        return KaFirCompoundSymbolResolutionError(backingAttempts = merged)
    }

    @Deprecated(
        message = "Use `KtSimpleNameExpression` instead",
        replaceWith = ReplaceWith("(element as? KtSimpleNameExpression)?.isImplicitReferenceToCompanion() == true"),
    )
    override fun isImplicitReferenceToCompanion(reference: KtReference): Boolean = withPsiValidityAssertion(reference.element) {
        (reference.element as? KtSimpleNameExpression)?.let { isImplicitReferenceToCompanion(it) } == true
    }

    private companion object {
        private val nonCallBinaryOperator: TokenSet = TokenSet.create(
            KtTokens.ELVIS,
            KtTokens.EQEQEQ,
            KtTokens.EXCLEQEQEQ,
            KtTokens.ANDAND,
            KtTokens.OROR,
        )
    }

    override fun isImplicitReferenceToCompanion(simpleNameExpression: KtSimpleNameExpression): Boolean =
        simpleNameExpression.withPsiValidityAssertion {
            isImplicitReferenceToCompanionImpl(simpleNameExpression)
        }

    override fun contextSensitiveResolutionStatus(simpleNameExpression: KtSimpleNameExpression): KaContextSensitiveResolutionStatus =
        simpleNameExpression.withPsiValidityAssertion {
            contextSensitiveResolutionStatusImpl(simpleNameExpression)
        }

    private fun performSymbolResolution(psi: KtElement): KaSymbolResolutionAttempt? = wrapError(psi) {
        when (psi) {
            // A user type redirects to its inner reference expression, which is cached on its own,
            // so we don't store a duplicate cache entry for the user type itself.
            is KtUserType -> psi.referenceExpression?.let(::performSymbolResolution)
            // A nullable type strips the nullability marker and resolves through its inner type element.
            is KtNullableType -> psi.innerType?.let(::performSymbolResolution)
            // A type reference delegates to the inner type element. Dynamic and intersection types
            // are not `KtResolvable`, so this redirect doesn't reach them — for those `typeElement`
            // values, `performSymbolResolution` falls through and returns `null`.
            is KtTypeReference -> psi.typeElement?.let(::performSymbolResolution)
            // A class literal expression (`Foo::class`) resolves to the classifier on its left-hand side.
            // The receiver is a name reference or qualified expression, both of which are already cached.
            is KtClassLiteralExpression -> psi.receiverExpression?.let(::performSymbolResolution)
            // A no-parens super-type entry (`class Foo : Bar`) resolves through its type reference.
            is KtSuperTypeEntry -> psi.typeReference?.let(::performSymbolResolution)
            // A delegated super-type entry (`class Foo : Bar by baz`) resolves through its type reference;
            // the `by` delegate expression is intentionally not the resolution target.
            is KtDelegatedSuperTypeEntry -> psi.typeReference?.let(::performSymbolResolution)
            else -> analysisSession.cacheStorage.resolveSymbolCache.value.getOrPut(psi) {
                resolveSymbol(psi)
            }
        }
    }

    private fun performCallResolution(psi: KtElement): KaCallResolutionAttempt? = wrapError(psi) {
        analysisSession.cacheStorage.resolveCallCache.value.getOrPut(psi) {
            val attempts = resolveCall(
                psi,
                onError = { psiToResolve, resolveFragmentOfCall ->
                    listOf(
                        transformErrorReference(
                            psi = psiToResolve,
                            call = this,
                            diagnosticHolder = this,
                            calleeReference = null,
                            resolveFragmentOfCall = resolveFragmentOfCall,
                        )
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

    private fun performCallCandidatesCollection(psi: KtElement): List<KaCallCandidate> = wrapError(psi) {
        resolveCall(
            psi,
            onError = { _, _ -> emptyList() },
            onSuccess = { psiToResolve, resolveCalleeExpressionOfFunctionCall, resolveFragmentOfCall ->
                collectCallCandidates(
                    psiToResolve,
                    resolveCalleeExpressionOfFunctionCall,
                    resolveFragmentOfCall
                )
            }
        )
    }

    internal val equalsSymbolInAny: FirNamedFunctionSymbol? by lazy(LazyThreadSafetyMode.PUBLICATION) {
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
        onError: FirDiagnosticHolder.(psiToResolve: KtElement, resolveFragmentOfCall: Boolean) -> List<T>,
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
            ?: psi.getConstructorCallForNameReferenceExpression()
            ?: psi.getContainingCallableReferenceExpressionForCalleeExpression()
            ?: psi

        val resolveFragmentOfCall = psiToResolve == containingBinaryExpressionForLhs || psiToResolve == containingUnaryExpressionForIncOrDec
        return when (val fir = psiToResolve.getOrBuildFirWithAdjustments()) {
            null -> emptyList()
            // Type references are not supposed to be covered by the call resolution. The symbol resolution will be used instead
            is FirResolvedTypeRef if psiToResolve is KtSimpleNameExpression -> emptyList()
            is FirDiagnosticHolder -> fir.onError(psiToResolve, resolveFragmentOfCall)
            else -> {
                fir.onSuccess(
                    psiToResolve,
                    psiToResolve == containingCallExpressionForCalleeExpression,
                    resolveFragmentOfCall,
                )
            }
        }
    }

    internal val stringPlusSymbol by lazy(LazyThreadSafetyMode.PUBLICATION) {
        findStringPlusSymbol(analysisSession.firSession)
    }

    /**
     * When resolving the callableReference of a [KtCallableReferenceExpression], we resolve the entire [KtCallableReferenceExpression] instead.
     * This way, the corresponding FIR element is the [FirFunctionCall], etc.
     */
    private fun KtElement.getContainingCallableReferenceExpressionForCalleeExpression(): KtCallableReferenceExpression? {
        if (this !is KtSimpleNameExpression) return null

        val callableReferenceExpression = parent as? KtCallableReferenceExpression ?: return null
        return callableReferenceExpression.takeIf { it.callableReference == this }
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

    /**
     * When resolving [KtNameReferenceExpression], we instead resolve the containing [KtConstructorCalleeExpression].
     * This way the corresponding FIR element is a call instead of the reference
     *
     * ### Example:
     *
     * ```kotlin
     * open class A
     * class B: A()
     * ```
     *
     * Here `A()` is represented as:
     * - SUPER_TYPE_CALL_ENTRY (`A()`)
     *   - CONSTRUCTOR_CALLEE (`A`)
     *     - TYPE_REFERENCE (`A`)
     *       - USER_TYPE (`A`)
     *         - REFERENCE_EXPRESSION (`A`)
     *
     * As a result, the reference expression cannot be resolved to a constructor call since regular unwraps like [getContainingCallExpressionForCalleeExpression]
     * is not enough to traverse through [KtTypeReference].
     *
     * The same is applicable for [KtAnnotationEntry].
     */
    private fun KtElement.getConstructorCallForNameReferenceExpression(): KtConstructorCalleeExpression? {
        if (this !is KtNameReferenceExpression) {
            return null
        }

        val userType = parent as? KtUserType ?: return null

        // We could consider only one level of KtUserType since only in this case it is basically a "constructor callee".
        // Otherwise, it is just a part of the qulified name
        val typeReference = userType.parent as? KtTypeReference ?: return null
        return typeReference.parent as? KtConstructorCalleeExpression
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

    private fun FirResolvedQualifier.toKaCallCandidates(): List<KaCallCandidate> {
        return toKaCalls(findQualifierConstructors()).map {
            KaFirInapplicableCallCandidate(
                backingCandidate = it,
                backingIsInBestCandidates = false,
                backingDiagnostic = inapplicableCandidateDiagnostic()
            )
        }
    }

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
            ?: KaNonBoundToPsiErrorDiagnostic(factoryName = FirErrors.OTHER_ERROR.name, diagnostic.reason, token)

        return KaFirInapplicableCallCandidate(
            backingCandidate = call,
            backingIsInBestCandidates = isInBestCandidates,
            backingDiagnostic = kaDiagnostic,
        )
    }

    private inline fun <R> wrapError(element: KtElement, action: () -> R): R = try {
        action()
    } catch (e: Exception) {
        rethrowExceptionWithDetails(
            "Error during resolving call ${element::class}",
            exception = e,
        ) {
            withPsiEntry("psi", element) { context(analysisSession) { it.kaModule } }
            element.getOrBuildFir(resolutionFacade)?.let { withFirEntry("fir", it) }
        }
    }
}
