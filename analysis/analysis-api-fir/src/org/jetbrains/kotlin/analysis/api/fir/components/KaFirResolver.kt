/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostic
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.components.resolver.*
import org.jetbrains.kotlin.analysis.api.fir.getCandidateSymbols
import org.jetbrains.kotlin.analysis.api.fir.isInvokeFunction
import org.jetbrains.kotlin.analysis.api.fir.resolution.*
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirArrayOfSymbolProvider.arrayOfSymbol
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirEmptySubstitutor
import org.jetbrains.kotlin.analysis.api.fir.utils.KaNonBoundToPsiErrorDiagnostic
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.processEqualsFunctions
import org.jetbrains.kotlin.analysis.api.fir.utils.withSymbolAttachment
import org.jetbrains.kotlin.analysis.api.impl.base.util.withPsiEntry
import org.jetbrains.kotlin.analysis.api.internals.KaInternalsResolver
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.projectStructure.kaModule
import org.jetbrains.kotlin.analysis.api.resolution.*
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaFunctionSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableSignature
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbolOfTypeSafe
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolver.AllCandidatesResolver
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.findStringPlusSymbol
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnosticHolder
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCall
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
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
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.fir.utils.exceptions.withFirSymbolEntry
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.KtPsiUtil.deparenthesize
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getPossiblyQualifiedCallExpression
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.jetbrains.kotlin.resolution.KtResolvable
import org.jetbrains.kotlin.resolution.KtResolvableCall
import org.jetbrains.kotlin.resolve.ArrayFqNames
import org.jetbrains.kotlin.resolve.calls.inference.buildCurrentSubstitutor
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.toKtPsiSourceElement
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.util.OperatorNameConventions.EQUALS
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.kotlin.utils.exceptions.*

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

    private fun KtBinaryExpression.getCompoundAssignKind(): KaCompoundAssignOperation.Kind = when (operationToken) {
        KtTokens.PLUSEQ -> KaCompoundAssignOperation.Kind.PLUS_ASSIGN
        KtTokens.MINUSEQ -> KaCompoundAssignOperation.Kind.MINUS_ASSIGN
        KtTokens.MULTEQ -> KaCompoundAssignOperation.Kind.TIMES_ASSIGN
        KtTokens.PERCEQ -> KaCompoundAssignOperation.Kind.REM_ASSIGN
        KtTokens.DIVEQ -> KaCompoundAssignOperation.Kind.DIV_ASSIGN
        else -> error("unexpected operator $operationToken")
    }

    private fun KtUnaryExpression.getInOrDecOperationKind(): KaCompoundUnaryOperation.Kind = when (operationToken) {
        KtTokens.PLUSPLUS -> KaCompoundUnaryOperation.Kind.INC
        KtTokens.MINUSMINUS -> KaCompoundUnaryOperation.Kind.DEC
        else -> error("unexpected operator $operationToken")
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
            return KaFirSimpleCallResolutionSuccess(
                backingCall = KaFirSimpleFunctionCall(
                    backingPartiallyAppliedSymbol = KaFirPartiallyAppliedSymbol(
                        backingSignature = signature,
                        dispatchReceiver = KaFirExplicitReceiverValue(
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

        handleMissedConstructorCall(this, psi)?.let { return it }

        if (this is FirImplicitInvokeCall) {

            // If we have a PSI expression like `Foo.Bar.Baz()` and try to resolve `Bar` part,
            // and the only FIR that we have for that PSI is an implicit invoke call, that means that
            // `Foo.Bar` is definitely not a property access - otherwise it would have had its own FIR.
            // So, it does not make sense to try to resolve such parts of qualifiers as KaSimpleCallResolutionSuccess
            // Binary expressions are accepted as they could be resolved into implicit invoke calls (in error cases)
            if ((psi as? KtExpression)?.getPossiblyQualifiedCallExpression() == null && psi !is KtBinaryExpression) {
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
        ): KaSimpleCallResolutionError where T : FirNamedReference, T : FirDiagnosticHolder = transformErrorReference(
            psi = psi,
            call = call,
            diagnosticHolder = calleeReference,
            calleeReference = calleeReference,
            resolveFragmentOfCall = resolveFragmentOfCall,
        )

        return when (this) {
            // FIR does not resolve to a symbol for equality calls.
            is FirEqualityOperatorCall -> toKaResolutionAttempt(psi)
            is FirCollectionLiteral -> toKaResolutionAttempt()
            is FirComparisonExpression -> compareToCall.toKaResolutionAttempt(
                psi,
                resolveCalleeExpressionOfFunctionCall,
                resolveFragmentOfCall
            )

            is FirSafeCallExpression -> unwrapSelector().toKaResolutionAttempt(
                psi,
                resolveCalleeExpressionOfFunctionCall,
                resolveFragmentOfCall,
            )

            is FirSmartCastExpression -> originalExpression.toKaResolutionAttempt(
                psi, resolveCalleeExpressionOfFunctionCall, resolveFragmentOfCall
            )

            is FirWhileLoop if psi is KtForExpression -> resolveForLoopCall(this, psi)
            is FirProperty if psi is KtPropertyDelegate -> resolveDelegatedPropertyCall(this, psi)

            else -> when (val calleeReference = toReference(analysisSession.firSession)) {
                is FirResolvedErrorReference -> transformErrorReference(this, calleeReference)
                is FirResolvedNamedReference -> when (calleeReference.resolvedSymbol) {
                    // `calleeReference.resolvedSymbol` isn't guaranteed to be callable. For example, function type parameters used in
                    // expression positions (e.g. `T` in `println(T)`) are parsed as `KtSimpleNameExpression` and built into
                    // `FirPropertyAccessExpression` (which is `FirResolvable`).
                    is FirCallableSymbol<*> -> createKaCallResolutionAttempt(
                        psi = psi,
                        fir = this,
                        calleeReference = calleeReference,
                        candidate = null,
                        resolveFragmentOfCall = resolveFragmentOfCall,
                    )

                    else -> null
                }

                is FirErrorNamedReference -> transformErrorReference(this, calleeReference)
                // Unresolved delegated constructor call is untransformed and end up as an `FirSuperReference`
                is FirSuperReference -> {
                    val delegatedConstructorCall = this as? FirDelegatedConstructorCall ?: return null
                    val errorTypeRef = delegatedConstructorCall.constructedTypeRef as? FirErrorTypeRef ?: return null
                    val sourceElement = errorTypeRef.source ?: source ?: psi.toKtPsiSourceElement()
                    val kaDiagnostic = errorTypeRef.diagnostic.asKaDiagnostic(sourceElement) ?: return null
                    KaFirSimpleCallResolutionError(
                        backedDiagnostic = kaDiagnostic,
                        backingCandidateCalls = emptyList(),
                    )
                }

                // A workaround to support desugared assignment where the lhs is an object, so the result is the operation itself
                // E.g., `++MyObject`
                // `resolveFragmentOfCall` must be true to not resolve `MyObject` into the operator
                null if (!resolveFragmentOfCall && this is FirVariableAssignment && lValue is FirDesugaredAssignmentValueReferenceExpression) -> {
                    rValue.toKaResolutionAttempt(
                        psi = psi,
                        resolveCalleeExpressionOfFunctionCall = resolveCalleeExpressionOfFunctionCall,
                        resolveFragmentOfCall = false,
                    )
                }

                else -> null
            }
        }
    }

    /**
     * By default, [KtCallExpression] is expected to be resolved to a callable, so if [fir] is [FirResolvedQualifier]
     * then, mostlikely, the call was missing `()`, so we try to resolve it as an error constructor call to
     * provide as much useful information as possible.
     *
     * But there are some exceptions:
     *
     * - [KtCallableReferenceExpression] could have [KtCallExpression] as a receiver and this is a valid code
     *     - `MyClassWithType<Int>::member`
     *
     * - [KtDotQualifiedExpression] could have [KtCallExpression] as a receiver and this is a valid code if the resolved symbol is static due to KTLC-390
     *     - `MyJavaClass<Int>.staticMethod()`
     *     - It could be dropped after the 2.5 version
     */
    private fun handleMissedConstructorCall(fir: FirElement, psi: KtElement): KaSimpleCallResolutionError? {
        if (fir !is FirResolvedQualifier) {
            return null
        }

        val callExpression = when (psi) {
            is KtQualifiedExpression if psi.selectorExpression is KtCallExpression -> psi
            is KtCallExpression -> psi.getQualifiedExpressionForSelectorOrThis()
            else -> return null
        }

        when (val parent = callExpression.parent) {
            is KtCallableReferenceExpression -> return null
            is KtDotQualifiedExpression -> when {
                // The workaround is required only for the receiver position, and it also helps to avoid infinite recursion
                parent.receiverExpression != callExpression -> {}

                // The workaround is required only without the feature
                analysisSession.firSession.languageVersionSettings.supportsFeature(LanguageFeature.ForbidUselessTypeArgumentsIn26) -> {}
                else -> return null
            }
        }

        val constructors = fir.findQualifierConstructors()
        val calls = fir.toKaCalls(constructors)
        return KaFirSimpleCallResolutionError(
            backedDiagnostic = inapplicableCandidateDiagnostic(),
            backingCandidateCalls = calls,
        )
    }

    private fun inapplicableCandidateDiagnostic(): KaDiagnostic {
        return KaNonBoundToPsiErrorDiagnostic(factoryName = FirErrors.OTHER_ERROR.name, "Inapplicable candidate", token)
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

    private fun createKaCall(
        psi: KtElement,
        fir: FirResolvable,
        candidate: Candidate?,
        resolveFragmentOfCall: Boolean,
    ): KaSimpleOrMultiCall? = createKaCallResolutionAttempt(
        psi = psi,
        fir = fir,
        calleeReference = fir.calleeReference,
        candidate = candidate,
        resolveFragmentOfCall = resolveFragmentOfCall,
    )?.successful

    private fun Candidate.toFirTypeArgumentsMapping(symbol: FirCallableSymbol<*>): Map<FirTypeParameterSymbol, ConeKotlinType> {
        val typeParameters = symbol.typeParameterSymbols.ifEmpty { return emptyMap() }

        // Maps a type parameter `A` into a type variable `TypeVariable(A)` type
        val candidateSubstitutor = substitutor

        // Maps the type variable `TypeVariable(A)` into the resulting type
        val systemSubstitutor = system.asReadOnlyStorage().buildCurrentSubstitutor(system, emptyMap()) as ConeSubstitutor

        val typeMapping = typeArgumentMapping as? TypeArgumentMapping.Mapped
        return buildMap {
            for ([index, parameterSymbol] in typeParameters.withIndex()) {
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

    private class TypeArgumentsMappingResult(
        val targetSymbol: FirCallableSymbol<*>,
        val firTypeArgumentsMapping: Map<FirTypeParameterSymbol, ConeKotlinType>,
        val typeArgumentsMapping: Map<KaTypeParameterSymbol, KaType>,
    )

    private fun computeTypeArgumentsMapping(
        fir: FirElement,
        calleeReference: FirReference?,
        candidate: Candidate?,
    ): TypeArgumentsMappingResult? {
        val targetSymbol = candidate?.symbol
            ?.takeUnless { it.origin is FirDeclarationOrigin.Synthetic.FakeFunction }
            ?: calleeReference?.toResolvedBaseSymbol()
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
        return TypeArgumentsMappingResult(targetSymbol, firTypeArgumentsMapping, typeArgumentsMapping)
    }

    private fun createKaCallResolutionAttempt(
        psi: KtElement,
        fir: FirElement,
        calleeReference: FirReference?,
        candidate: Candidate?,
        resolveFragmentOfCall: Boolean,
    ): KaCallResolutionAttempt? {
        if (fir is FirSmartCastExpression) {
            return (fir.originalExpression as? FirResolvable)?.let {
                createKaCallResolutionAttempt(
                    psi = psi,
                    fir = it,
                    calleeReference = calleeReference,
                    candidate = candidate,
                    resolveFragmentOfCall = resolveFragmentOfCall,
                )
            }
        }

        val mappingResult = computeTypeArgumentsMapping(fir, calleeReference, candidate) ?: return null
        return handleCompoundAccessCall(
            psi = psi,
            fir = fir,
            resolveFragmentOfCall = resolveFragmentOfCall,
            typeArgumentsMapping = mappingResult.typeArgumentsMapping,
        ) ?: buildKaCall(
            psi = psi,
            fir = fir,
            calleeReference = calleeReference,
            candidate = candidate,
            mappingResult = mappingResult,
        )?.let(::KaFirSimpleCallResolutionSuccess)
    }

    /**
     * Core call construction logic. Assumes the caller has already handled compound access.
     */
    private fun buildKaCall(
        psi: KtElement,
        fir: FirElement,
        calleeReference: FirReference?,
        candidate: Candidate?,
        mappingResult: TypeArgumentsMappingResult,
    ): KaSimpleCall<*, *>? {
        val targetSymbol = mappingResult.targetSymbol
        val firTypeArgumentsMapping = mappingResult.firTypeArgumentsMapping
        val typeArgumentsMapping = mappingResult.typeArgumentsMapping

        val signature = with(analysisSession) {
            val substitutor = substitutorByMap(firTypeArgumentsMapping, firSession).toKaSubstitutor()

            // This is crucial to create a signature by Fir symbol as it can be call-site substitution
            val unsubstitutedSignature = targetSymbol.toKaSignature()
            unsubstitutedSignature.substitute(substitutor)
        }

        var argumentsHaveExtensionReceiver = false
        var argumentsContextParameterCount = 0
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
            KaFirImplicitInvokeCall(
                backingPartiallyAppliedSymbol = partiallyAppliedSymbol as KaPartiallyAppliedFunctionSymbol<KaNamedFunctionSymbol>,
                backingArgumentMapping = argumentMapping,
                backingTypeArgumentsMapping = typeArgumentsMapping,
            )
        } else {
            KaFirSimpleFunctionCall(
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
                        withPsiEntry("psi", psi) { context(analysisSession) { it.kaModule } }
                    }

                is KtExpression -> psi
                else -> errorWithAttachment("unexpected PSI ${psi::class.simpleName} for FirImplicitInvokeCall") {
                    withPsiEntry("psi", psi) { context(analysisSession) { it.kaModule } }
                }
            }

            if (explicitReceiverPsi is KtCallExpression) {
                explicitReceiverPsi = explicitReceiverPsi.calleeExpression
                    ?: errorWithAttachment("missing calleeExpression in PSI ${psi::class.simpleName} for FirImplicitInvokeCall") {
                        withPsiEntry("psi", psi) { context(analysisSession) { it.kaModule } }
                    }
            }

            // Specially handle @ExtensionFunctionType and @ContextFunctionTypeParams
            dispatchReceiver?.resolvedType?.let { resolvedType ->
                argumentsHaveExtensionReceiver = resolvedType.isExtensionFunctionType
                argumentsContextParameterCount = resolvedType.contextParameterNumberForFunctionType
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

                    dispatchReceiverValue = KaFirExplicitReceiverValue(
                        expression = explicitReceiverPsi,
                        backingType = dispatchReceiver.resolvedType.asKaType(),
                        isSafeNavigation = false,
                    )

                    extensionReceiverValue = if (argumentsHaveExtensionReceiver) {
                        when (fir) {
                            is FirFunctionCall -> fir.arguments.drop(argumentsContextParameterCount).firstOrNull()?.toKaReceiverValue()
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
                    extensionReceiverValue = KaFirExplicitReceiverValue(
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

            // In regular invoke functions (explicitly declared operator invoke functions) context arguments are available on the fir call,
            // while for functional types they explicitly passed as regular arguments
            val adjustedContextArguments = contextArguments.ifEmpty {
                (fir as? FirFunctionCall)?.arguments?.take(argumentsContextParameterCount).orEmpty()
            }

            return KaFirPartiallyAppliedSymbol(
                backingSignature = signature,
                dispatchReceiver = dispatchReceiverValue,
                extensionReceiver = extensionReceiverValue,
                contextArguments = adjustedContextArguments.toKaContextParameterValues(),
            )
        }

        val partiallyAppliedSymbol = when {
            candidate != null -> when {
                fir is FirImplicitInvokeCall ||
                        calleeReference?.calleeOrCandidateName != OperatorNameConventions.INVOKE && targetSymbol.isInvokeFunction() -> {

                    // Implicit invoke (e.g., `x()`) will have a different callee symbol (e.g., `x`) than the candidate (e.g., `invoke`).
                    createKtPartiallyAppliedSymbolForImplicitInvoke(
                        dispatchReceiver = candidate.dispatchReceiver?.expression,
                        extensionReceiver = candidate.chosenExtensionReceiver?.expression,
                        explicitReceiverKind = candidate.explicitReceiverKind,
                        contextArguments = candidate.contextArguments(),
                    )
                }

                else -> KaFirPartiallyAppliedSymbol(
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

            fir is FirQualifiedAccessExpression -> KaFirPartiallyAppliedSymbol(
                backingSignature = signature,
                dispatchReceiver = fir.dispatchReceiver?.toKaReceiverValue(),
                extensionReceiver = fir.extensionReceiver?.toKaReceiverValue(),
                contextArguments = fir.contextArguments.toKaContextParameterValues(),
            )

            fir is FirVariableAssignment -> KaFirPartiallyAppliedSymbol(
                backingSignature = signature,
                dispatchReceiver = fir.dispatchReceiver?.toKaReceiverValue(),
                extensionReceiver = fir.extensionReceiver?.toKaReceiverValue(),
                contextArguments = fir.contextArguments.toKaContextParameterValues(),
            )

            fir is FirDelegatedConstructorCall -> KaFirPartiallyAppliedSymbol(
                backingSignature = signature,
                dispatchReceiver = fir.dispatchReceiver?.toKaReceiverValue(),
                extensionReceiver = null,
                contextArguments = fir.contextArguments.toKaContextParameterValues(),
            )

            else -> KaFirPartiallyAppliedSymbol(
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
                KaFirAnnotationCall(
                    backingPartiallyAppliedSymbol = partiallyAppliedSymbol as KaPartiallyAppliedFunctionSymbol<KaConstructorSymbol>,
                    backingArgumentMapping = fir.createArgumentMapping(signatureOfCallee = partiallyAppliedSymbol.signature as KaFunctionSignature<*>),
                )
            }

            is FirDelegatedConstructorCall -> {
                if (partiallyAppliedSymbol.symbol !is KaConstructorSymbol) return null
                @Suppress("UNCHECKED_CAST") // safe because of the above check on targetKtSymbol
                KaFirDelegatedConstructorCall(
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
                KaFirSimpleVariableAccessCall(
                    backingPartiallyAppliedSymbol = partiallyAppliedSymbol as KaPartiallyAppliedVariableSymbol<KaVariableSymbol>,
                    backingTypeArgumentsMapping = typeArgumentsMapping,
                    backingKind = KaFirVariableWriteAccess(value = rhs),
                    backingIsContextSensitive = calleeReference?.isContextSensitive == true,
                )
            }

            is FirCallableReferenceAccess -> KaFirCallableReferenceCall(
                backingPartiallyAppliedSymbol = partiallyAppliedSymbol,
                backingTypeArgumentsMapping = typeArgumentsMapping,
            )

            is FirPropertyAccessExpression,
            is FirFunctionCall,
            is FirErrorExpression,
            is FirResolvedErrorReference,
                -> when (partiallyAppliedSymbol.symbol) {

                is KaVariableSymbol -> {
                    @Suppress("UNCHECKED_CAST") // safe because of the above check on targetKtSymbol
                    KaFirSimpleVariableAccessCall(
                        backingPartiallyAppliedSymbol = partiallyAppliedSymbol as KaPartiallyAppliedVariableSymbol<KaVariableSymbol>,
                        backingTypeArgumentsMapping = typeArgumentsMapping,
                        backingKind = KaFirVariableReadAccess,
                        backingIsContextSensitive = calleeReference?.isContextSensitive == true,
                    )
                }

                is KaFunctionSymbol -> {
                    val argumentMapping = if (candidate is Candidate) {
                        runIf(candidate.argumentMappingInitialized) { candidate.argumentMapping.unwrapAtoms() }
                    } else {
                        (fir as? FirCall)?.resolvedArgumentMappingIncludingContextArguments
                    }

                    val argumentCountToDrop = argumentsContextParameterCount + (if (argumentsHaveExtensionReceiver) 1 else 0)
                    val argumentMappingWithoutExtensionReceiverAndContextArguments = argumentMapping?.entries?.drop(argumentCountToDrop)

                    @Suppress("UNCHECKED_CAST") // safe because of the above check on targetKtSymbol
                    buildFunctionCall(
                        partiallyAppliedSymbol = partiallyAppliedSymbol as KaPartiallyAppliedFunctionSymbol<KaFunctionSymbol>,
                        argumentMapping = argumentMappingWithoutExtensionReceiverAndContextArguments
                            ?.createArgumentMapping(partiallyAppliedSymbol.signature)
                            .orEmpty(),
                        typeArgumentsMapping = typeArgumentsMapping,
                    )
                }
            }

            else -> null
        }
    }

    /**
     * Handle compound assignment with array access convention
     */
    private fun createKaCallForArrayAccessConvention(
        psi: KtElement,
        fir: FirElement,
        accessExpression: KtExpression?,
        callProvider: (KtElement, FirFunctionCall, KtArrayAccessExpression) -> KaCallResolutionAttempt?,
    ): KaCallResolutionAttempt? {
        if (fir !is FirFunctionCall || fir.calleeReference.name != OperatorNameConventions.SET || accessExpression !is KtArrayAccessExpression) {
            return null
        }

        return callProvider(psi, fir, accessExpression)
    }

    /**
     * Handle compound assignment with variable
     */
    private fun createKaCallForVariableAccessConvention(
        psi: KtElement,
        fir: FirElement,
        accessExpression: KtExpression?,
        resolveFragmentOfCall: Boolean,
        typeArgumentsMapping: Map<KaTypeParameterSymbol, KaType>,
        compoundOperationProvider: (KaFunctionCall<KaNamedFunctionSymbol>) -> KaCompoundOperation,
        rhsExpression: KtExpression?,
    ): KaCallResolutionAttempt? {
        if (fir !is FirVariableAssignment || accessExpression !is KtQualifiedExpression && accessExpression !is KtNameReferenceExpression) {
            return null
        }

        val variableSymbol = fir.toPartiallyAppliedSymbol() ?: return null
        val variableAccessCall = KaFirSimpleVariableAccessCall(
            backingPartiallyAppliedSymbol = variableSymbol,
            backingTypeArgumentsMapping = typeArgumentsMapping,
            backingKind = KaFirVariableReadAccess,
            backingIsContextSensitive = fir.calleeReference?.isContextSensitive == true,
        )

        if (resolveFragmentOfCall) {
            return KaFirSimpleCallResolutionSuccess(backingCall = variableAccessCall)
        }

        // Extract operation call
        val firOperationCall = fir.rValue as? FirFunctionCall
            ?: getInitializerOfReferencedLocalVariable(fir.rValue) ?: return null


        val operationError = findErrorCall(firOperationCall, psi)
        val operationAttempt: KaSimpleCallResolutionAttempt = if (operationError != null) {
            operationError
        } else {
            val operationCall = buildOperationCallForCompoundVariableAccess(firOperationCall, accessExpression, rhsExpression)
                ?: return null
            KaFirSimpleCallResolutionSuccess(backingCall = operationCall)
        }

        val variableAttempt = KaFirSimpleCallResolutionSuccess(backingCall = variableAccessCall)
        return KaFirCompoundVariableAccessCallResolutionAttempt(
            backingCompoundOperationProvider = compoundOperationProvider,
            backingVariableCallAttempt = variableAttempt,
            backingOperationCallAttempt = operationAttempt,
        )
    }

    private fun createKaCallForCompoundAccessConvention(
        psi: KtElement,
        fir: FirElement,
        accessExpression: KtExpression?,
        rhsExpression: KtExpression?,
        resolveFragmentOfCall: Boolean,
        typeArgumentsMapping: Map<KaTypeParameterSymbol, KaType>,
        callProvider: (KtElement, FirFunctionCall, KtArrayAccessExpression) -> KaCallResolutionAttempt?,
        compoundOperationProvider: (KaFunctionCall<KaNamedFunctionSymbol>) -> KaCompoundOperation,
    ): KaCallResolutionAttempt? = createKaCallForArrayAccessConvention(
        psi = psi,
        fir = fir,
        accessExpression = accessExpression,
        callProvider = callProvider,
    ) ?: createKaCallForVariableAccessConvention(
        psi = psi,
        fir = fir,
        accessExpression = accessExpression,
        rhsExpression = rhsExpression,
        resolveFragmentOfCall = resolveFragmentOfCall,
        typeArgumentsMapping = typeArgumentsMapping,
        compoundOperationProvider = compoundOperationProvider,
    )

    private fun transformErrorReference(
        psi: KtElement,
        call: FirElement,
        diagnosticHolder: FirDiagnosticHolder,
        calleeReference: FirNamedReference?,
        resolveFragmentOfCall: Boolean,
    ): KaSimpleCallResolutionError {
        val diagnostic = diagnosticHolder.diagnostic
        val kaDiagnostic = diagnosticHolder.createKaDiagnostic()

        if (diagnostic is ConeHiddenCandidateError) {
            return KaFirSimpleCallResolutionError(
                backedDiagnostic = kaDiagnostic,
                backingCandidateCalls = emptyList(),
            )
        }

        val candidateCalls = if (diagnostic is ConeDiagnosticWithCandidates) {
            diagnostic.candidates.mapNotNull {
                if (it is Candidate) {
                    val attempt = createKaCallResolutionAttempt(
                        psi = psi,
                        fir = call,
                        calleeReference = calleeReference,
                        candidate = it,
                        resolveFragmentOfCall = resolveFragmentOfCall,
                    ) as? KaSimpleCallResolutionSuccess

                    attempt?.call
                } else {
                    null
                }
            }
        } else {
            val attempt = createKaCallResolutionAttempt(
                psi = psi,
                fir = call,
                calleeReference = calleeReference,
                candidate = null,
                resolveFragmentOfCall = resolveFragmentOfCall
            ) as? KaSimpleCallResolutionSuccess

            listOfNotNull(attempt?.call)
        }

        return KaFirSimpleCallResolutionError(
            backedDiagnostic = kaDiagnostic,
            backingCandidateCalls = candidateCalls,
        )
    }

    private fun FirExpression.asFunctionOperatorCall(
        expectedSourceKind: KtFakeSourceElementKind,
    ): FirFunctionCall? = (this as? FirFunctionCall)?.takeIf {
        it.origin == FirFunctionCallOrigin.Operator && it.source?.kind == expectedSourceKind
    }

    private fun resolveForLoopCall(firLoop: FirWhileLoop, psi: KtForExpression): KaCallResolutionAttempt? {
        val firHasNextCall = firLoop.condition.asFunctionOperatorCall(KtFakeSourceElementKind.DesugaredForLoop) ?: return null

        val iteratorPropertyAccess = firHasNextCall.explicitReceiver as? FirQualifiedAccessExpression ?: return null
        val iteratorPropertySymbol = (iteratorPropertyAccess.calleeReference as? FirResolvedNamedReference)
            ?.resolvedSymbol as? FirPropertySymbol ?: return null

        val firIteratorCall = iteratorPropertySymbol.fir
            .initializer
            ?.asFunctionOperatorCall(KtFakeSourceElementKind.DesugaredForLoop)
            ?: return null

        val firNextCall = (firLoop.block.statements.firstOrNull() as? FirProperty)
            ?.initializer
            ?.asFunctionOperatorCall(KtFakeSourceElementKind.DesugaredForLoop)
            ?: return null

        val iteratorAttempt = resolveSingleSubCall(firIteratorCall, psi)
        val hasNextAttempt = resolveSingleSubCall(firHasNextCall, psi)
        val nextAttempt = resolveSingleSubCall(firNextCall, psi)

        return KaFirForLoopCallResolutionAttempt(
            backingIteratorCallAttempt = iteratorAttempt,
            backingHasNextCallAttempt = hasNextAttempt,
            backingNextCallAttempt = nextAttempt,
        )
    }

    private fun resolveDelegatedPropertyCall(firProperty: FirProperty, psi: KtPropertyDelegate): KaCallResolutionAttempt? {
        if (firProperty.delegate == null) return null

        val firGetValueCall = (firProperty.getter?.body?.statements?.singleOrNull() as? FirReturnExpression)
            ?.result
            ?.asFunctionOperatorCall(KtFakeSourceElementKind.DelegatedPropertyAccessor.Getter)

        val firSetValueCall = (firProperty.setter?.body?.statements?.singleOrNull() as? FirReturnExpression)
            ?.result
            ?.asFunctionOperatorCall(KtFakeSourceElementKind.DelegatedPropertyAccessor.Setter)

        val firProvideDelegateCall = firProperty.delegate
            ?.asFunctionOperatorCall(KtFakeSourceElementKind.DelegatedPropertyAccessor.DelegateExpression)

        // The getter is mandatory
        if (firGetValueCall == null) return null

        val getterAttempt = resolveSingleSubCall(firGetValueCall, psi)
        val setterAttempt = firSetValueCall?.let { resolveSingleSubCall(it, psi) }
        val provideDelegateAttempt = firProvideDelegateCall?.let { resolveSingleSubCall(it, psi) }

        return KaFirDelegatedPropertyCallResolutionAttempt(
            backingValueGetterCallAttempt = getterAttempt,
            backingValueSetterCallAttempt = setterAttempt,
            backingProvideDelegateCallAttempt = provideDelegateAttempt,
        )
    }

    private fun findErrorCall(
        call: FirFunctionCall,
        psi: KtElement,
    ): KaSimpleCallResolutionError? = when (val ref = call.calleeReference) {
        is FirDiagnosticHolder -> transformErrorReference(
            psi = psi,
            call = call,
            diagnosticHolder = ref,
            calleeReference = ref,
            resolveFragmentOfCall = false,
        )

        else -> null
    }

    /**
     * Resolves a [FirFunctionCall] into a [KaSimpleCallResolutionAttempt].
     * If the call has an error, returns [KaSimpleCallResolutionError]; otherwise builds a [KaSimpleCallResolutionSuccess].
     */
    private fun resolveSingleSubCall(call: FirFunctionCall, psi: KtElement): KaSimpleCallResolutionAttempt {
        findErrorCall(call, psi)?.let { return it }

        return when (val kaCall = buildNamedFunctionCall(call)) {
            null -> KaFirSimpleCallResolutionError(
                backedDiagnostic = KaNonBoundToPsiErrorDiagnostic(
                    factoryName = FirErrors.OTHER_ERROR.name,
                    defaultMessage = "Failed to build call",
                    token = token,
                ),
                backingCandidateCalls = emptyList(),
            )

            else -> KaFirSimpleCallResolutionSuccess(backingCall = kaCall)
        }
    }

    private fun buildNamedFunctionCall(firFunctionCall: FirFunctionCall): KaFunctionCall<KaNamedFunctionSymbol>? {
        val functionSymbol = (firFunctionCall.calleeReference as? FirResolvedNamedReference)
            ?.resolvedSymbol as? FirNamedFunctionSymbol ?: return null

        val firTypeArgumentsMapping = firFunctionCall.toFirTypeArgumentsMapping(functionSymbol)
        val typeArgumentsMapping = firTypeArgumentsMapping.asKaTypeParametersMapping()

        val kaSignature = with(analysisSession) {
            val substitutor = substitutorByMap(firTypeArgumentsMapping, firSession).toKaSubstitutor()
            functionSymbol.toKaSignature().substitute(substitutor)
        }

        val partiallyAppliedSymbol = KaFirPartiallyAppliedSymbol(
            backingSignature = kaSignature,
            dispatchReceiver = firFunctionCall.dispatchReceiver?.toKaReceiverValue(),
            extensionReceiver = firFunctionCall.extensionReceiver?.toKaReceiverValue(),
            contextArguments = firFunctionCall.contextArguments.toKaContextParameterValues(),
        )

        @Suppress("UNCHECKED_CAST")
        return KaFirSimpleFunctionCall(
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
    ): KaCallResolutionAttempt? {
        return when (psi) {
            is KtBinaryExpression if psi.operationToken in KtTokens.AUGMENTED_ASSIGNMENTS -> {
                val rightOperandPsi = deparenthesize(psi.right) ?: return null
                val leftOperandPsi = deparenthesize(psi.left) ?: return null
                val compoundAssignKind = psi.getCompoundAssignKind()

                createKaCallForCompoundAccessConvention(
                    psi = psi,
                    fir = fir,
                    accessExpression = leftOperandPsi,
                    rhsExpression = rightOperandPsi,
                    resolveFragmentOfCall = resolveFragmentOfCall,
                    typeArgumentsMapping = typeArgumentsMapping,
                    callProvider = { psi, fir, arrayAccessExpression ->
                        createCompoundArrayAccessCall(
                            psi = psi,
                            firCall = fir,
                            lhsArrayAccessExpression = arrayAccessExpression,
                            rhsExpression = rightOperandPsi,
                            resolveFragmentOfCall = resolveFragmentOfCall,
                            compoundOperationProvider = { KaFirCompoundAssignOperation(it, compoundAssignKind, rightOperandPsi) },
                        )
                    },
                    compoundOperationProvider = { KaFirCompoundAssignOperation(it, compoundAssignKind, rightOperandPsi) },
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
                    psi = psi,
                    fir = fir,
                    accessExpression = baseExpression,
                    rhsExpression = null,
                    resolveFragmentOfCall = resolveFragmentOfCall,
                    typeArgumentsMapping = typeArgumentsMapping,
                    callProvider = { psi, firCall, ktExpression ->
                        createCompoundArrayAccessCall(
                            psi = psi,
                            firCall = firCall,
                            lhsArrayAccessExpression = ktExpression,
                            rhsExpression = null,
                            resolveFragmentOfCall = resolveFragmentOfCall,
                            compoundOperationProvider = { KaFirCompoundUnaryOperation(it, incOrDecOperationKind, precedence) },
                        )
                    },
                    compoundOperationProvider = { KaFirCompoundUnaryOperation(it, incOrDecOperationKind, precedence) },
                )
            }
            else -> null
        }
    }

    private fun createCompoundArrayAccessCall(
        psi: KtElement,
        firCall: FirFunctionCall,
        lhsArrayAccessExpression: KtArrayAccessExpression,
        rhsExpression: KtExpression?,
        resolveFragmentOfCall: Boolean,
        compoundOperationProvider: (KaFunctionCall<KaNamedFunctionSymbol>) -> KaCompoundOperation,
    ): KaCallResolutionAttempt? {
        // The last argument of `set` is the new value to be set. This value should be a call to the respective `plus`, `minus`,
        // `times`, `div`, or `rem` function.
        val firOperationCall = firCall.arguments.lastOrNull() as? FirFunctionCall ?: return null

        // The explicit receiver for both `get` and `set` call should be the array expression.
        val firExplicitReceiver = firOperationCall.explicitReceiver ?: return null
        val firGetCall = firExplicitReceiver as? FirFunctionCall // case for array access and prefix
            ?: getInitializerOfReferencedLocalVariable(firExplicitReceiver) // case for postfix
            ?: return null

        val indexExpressions = lhsArrayAccessExpression.indexExpressions
        val arrayExpression = lhsArrayAccessExpression.arrayExpression

        // Build getter call or error
        val getterError = findErrorCall(firGetCall, psi)
        val getterAttempt: KaSimpleCallResolutionAttempt
        if (getterError != null) {
            getterAttempt = getterError
        } else {
            val getPartiallyAppliedSymbol = firGetCall.toPartiallyAppliedSymbol(arrayExpression) ?: return null
            val getArgumentMapping = indexExpressions.zip(getPartiallyAppliedSymbol.signature.valueParameters).toMap()
            val getCall = KaFirSimpleFunctionCall(
                backingPartiallyAppliedSymbol = getPartiallyAppliedSymbol,
                backingArgumentMapping = getArgumentMapping,
                backingTypeArgumentsMapping = firCall
                    .toFirTypeArgumentsMapping(symbol = getPartiallyAppliedSymbol.symbol.firSymbol)
                    .asKaTypeParametersMapping(),
            ).let {
                @Suppress("UNCHECKED_CAST")
                it as KaFunctionCall<KaNamedFunctionSymbol>
            }

            getterAttempt = KaFirSimpleCallResolutionSuccess(backingCall = getCall)
        }

        if (resolveFragmentOfCall) {
            return getterAttempt
        }

        // Build operation call or error
        val operationError = findErrorCall(firOperationCall, psi)
        val operationAttempt: KaSimpleCallResolutionAttempt = if (operationError != null) {
            operationError
        } else {
            // The explicit receiver in this case is a synthetic FirFunctionCall to `get`, which does not have a corresponding PSI. So
            // we use the `lhsArrayAccessExpression` as the supplement.
            val operationPartiallyAppliedSymbol = firOperationCall.toPartiallyAppliedSymbol(lhsArrayAccessExpression) ?: return null
            val operationArgumentsMapping = listOfNotNull(lhsArrayAccessExpression, rhsExpression)
                .zip(operationPartiallyAppliedSymbol.signature.valueParameters)
                .toMap()
            val operationCall = KaFirSimpleFunctionCall(
                backingPartiallyAppliedSymbol = operationPartiallyAppliedSymbol,
                backingArgumentMapping = operationArgumentsMapping,
                backingTypeArgumentsMapping = firCall
                    .toFirTypeArgumentsMapping(symbol = operationPartiallyAppliedSymbol.symbol.firSymbol)
                    .asKaTypeParametersMapping(),
            ).let {
                @Suppress("UNCHECKED_CAST")
                it as KaFunctionCall<KaNamedFunctionSymbol>
            }

            KaFirSimpleCallResolutionSuccess(backingCall = operationCall)
        }

        // Build setter call or error
        val setterError = findErrorCall(firCall, psi)
        val setterAttempt: KaSimpleCallResolutionAttempt
        if (setterError != null) {
            setterAttempt = setterError
        } else {
            val setPartiallyAppliedSymbol = firCall.toPartiallyAppliedSymbol(arrayExpression) ?: return null
            val setArgumentsMapping = mapOf(indexExpressions.last() to setPartiallyAppliedSymbol.signature.valueParameters.last())
            val setCall = KaFirSimpleFunctionCall(
                backingPartiallyAppliedSymbol = setPartiallyAppliedSymbol,
                backingArgumentMapping = setArgumentsMapping,
                backingTypeArgumentsMapping = firCall
                    .toFirTypeArgumentsMapping(symbol = setPartiallyAppliedSymbol.symbol.firSymbol)
                    .asKaTypeParametersMapping(),
            ).let {
                @Suppress("UNCHECKED_CAST")
                it as KaFunctionCall<KaNamedFunctionSymbol>
            }

            setterAttempt = KaFirSimpleCallResolutionSuccess(backingCall = setCall)
        }

        return KaFirCompoundArrayAccessCallResolutionAttempt(
            backingCompoundOperationProvider = compoundOperationProvider,
            backingIndexArguments = indexExpressions,
            backingGetterCallAttempt = getterAttempt,
            backingOperationCallAttempt = operationAttempt,
            backingSetterCallAttempt = setterAttempt,
        )
    }

    private fun getInitializerOfReferencedLocalVariable(variableReference: FirExpression): FirFunctionCall? {
        return variableReference.toReference(resolutionFacade.useSiteFirSession)
            ?.toResolvedVariableSymbol()
            ?.fir
            ?.initializer as? FirFunctionCall
    }

    private fun buildOperationCallForCompoundVariableAccess(
        firOperationCall: FirFunctionCall,
        leftOperandPsi: KtExpression,
        rightOperandPsi: KtExpression?,
    ): KaFunctionCall<KaNamedFunctionSymbol>? {
        val operationPartiallyAppliedSymbol = firOperationCall.toPartiallyAppliedSymbol(leftOperandPsi) ?: return null

        val operationArgumentsMapping = listOfNotNull(leftOperandPsi, rightOperandPsi)
            .zip(operationPartiallyAppliedSymbol.signature.valueParameters)
            .toMap()

        val functionCall = KaFirSimpleFunctionCall(
            backingPartiallyAppliedSymbol = operationPartiallyAppliedSymbol,
            backingArgumentMapping = operationArgumentsMapping,
            backingTypeArgumentsMapping = firOperationCall
                .toFirTypeArgumentsMapping(symbol = operationPartiallyAppliedSymbol.symbol.firSymbol)
                .asKaTypeParametersMapping(),
        )

        @Suppress("UNCHECKED_CAST")
        return functionCall as KaFunctionCall<KaNamedFunctionSymbol>
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

    private fun FirResolvedQualifier.findQualifierConstructors(): List<FirConstructorSymbol> {
        val classSymbol = this.qualifierSymbol?.fullyExpandedClass(analysisSession.firSession) ?: return emptyList()
        return classSymbol.unsubstitutedScope(
            analysisSession.firSession,
            analysisSession.getScopeSessionFor(analysisSession.firSession),
            withForcedTypeCalculator = true,
            memberRequiredPhase = null,
        ).getDeclaredConstructors()
    }

    private fun FirResolvedQualifier.toKaCalls(constructors: List<FirConstructorSymbol>): List<KaFunctionCall<*>> =
        constructors.map { constructor ->
            val signature = constructor.toKaSignature()
            val partiallyAppliedSymbol = KaFirPartiallyAppliedSymbol(
                backingSignature = signature as KaFunctionSignature<*>,
                dispatchReceiver = null,
                extensionReceiver = null,
                contextArguments = emptyList(),
            )

            val firTypeArgumentsMapping = toFirTypeArgumentsMapping(constructor)
            val typeArgumentsMapping = firTypeArgumentsMapping.asKaTypeParametersMapping()
            KaFirSimpleFunctionCall(
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
                val partiallyAppliedSymbol = KaFirPartiallyAppliedSymbol(
                    backingSignature = with(useSiteSession) { defaultArrayOfSymbol.substitute(substitutor) },
                    dispatchReceiver = null,
                    extensionReceiver = null,
                    contextArguments = emptyList(),
                )

                KaFirSimpleCallResolutionError(
                    backedDiagnostic = unresolvedArrayOfDiagnostic,
                    backingCandidateCalls = listOf(
                        KaFirSimpleFunctionCall(
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
        val partiallyAppliedSymbol = KaFirPartiallyAppliedSymbol(
            backingSignature = with(analysisSession) { arrayOfSymbol.substitute(substitutor) },
            dispatchReceiver = null,
            extensionReceiver = null,
            contextArguments = emptyList(),
        )

        return KaFirSimpleCallResolutionSuccess(
            backingCall = KaFirSimpleFunctionCall(
                backingPartiallyAppliedSymbol = partiallyAppliedSymbol,
                backingArgumentMapping = createArgumentMapping(arrayOfSymbol, substitutor),
                backingTypeArgumentsMapping = toTypeArgumentsMapping(arrayOfSymbol),
            )
        )
    }

    private fun FirCollectionLiteral.createSubstitutorFromTypeArguments(arrayOfSymbol: KaNamedFunctionSymbol): KaSubstitutor {
        val firSymbol = arrayOfSymbol.firSymbol
        // No type parameter means this is an arrayOf call of primitives, in which case there is no type arguments
        val typeParameter = firSymbol.fir.typeParameters.singleOrNull() ?: return KaFirEmptySubstitutor(token)

        val elementType = resolvedType.arrayElementType() ?: return KaFirEmptySubstitutor(token)
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
                KaFirSimpleCallResolutionSuccess(
                    backingCall = KaFirSimpleFunctionCall(
                        backingPartiallyAppliedSymbol = KaFirPartiallyAppliedSymbol(
                            backingSignature = kaSignature,
                            dispatchReceiver = KaFirExplicitReceiverValue(
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
