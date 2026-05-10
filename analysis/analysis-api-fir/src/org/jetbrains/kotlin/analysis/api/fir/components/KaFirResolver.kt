/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostic
import org.jetbrains.kotlin.analysis.api.fir.*
import org.jetbrains.kotlin.analysis.api.fir.references.*
import org.jetbrains.kotlin.analysis.api.fir.references.FirReferenceResolveHelper.getQualifierSelected
import org.jetbrains.kotlin.analysis.api.fir.references.FirReferenceResolveHelper.getSymbolsByNameArgumentExpression
import org.jetbrains.kotlin.analysis.api.fir.references.FirReferenceResolveHelper.getSymbolsByResolvedImport
import org.jetbrains.kotlin.analysis.api.fir.references.FirReferenceResolveHelper.getSymbolsForResolvedQualifier
import org.jetbrains.kotlin.analysis.api.fir.references.FirReferenceResolveHelper.getSymbolsForResolvedTypeRef
import org.jetbrains.kotlin.analysis.api.fir.references.FirReferenceResolveHelper.toTargetSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirArrayOfSymbolProvider.arrayOfSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.processEqualsFunctions
import org.jetbrains.kotlin.analysis.api.fir.utils.withSymbolAttachment
import org.jetbrains.kotlin.analysis.api.getModule
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseResolver
import org.jetbrains.kotlin.analysis.api.impl.base.components.withPsiValidityAssertion
import org.jetbrains.kotlin.analysis.api.impl.base.resolution.*
import org.jetbrains.kotlin.analysis.api.impl.base.util.KaNonBoundToPsiErrorDiagnostic
import org.jetbrains.kotlin.analysis.api.impl.base.util.withPsiEntry
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.resolution.*
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaFunctionSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableSignature
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbolOfTypeSafe
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolver.AllCandidatesResolver
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.findStringPlusSymbol
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnosticHolder
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCall
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
import org.jetbrains.kotlin.idea.references.KDocReference
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.KtPsiUtil.deparenthesize
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getPossiblyQualifiedCallExpression
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.jetbrains.kotlin.psi.psiUtil.topParenthesizedParentOrMe
import org.jetbrains.kotlin.resolve.ArrayFqNames
import org.jetbrains.kotlin.resolve.calls.inference.buildCurrentSubstitutor
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.toKtPsiSourceElement
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.util.OperatorNameConventions.EQUALS
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
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

        return wholeQualifier is FirResolvedQualifier && wholeQualifier.resolvedToCompanionObject
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

    @OptIn(KtExperimentalApi::class)
    override fun performSymbolResolution(reference: KtReference): KaSymbolResolutionAttempt? {
        if (reference !is KaFirReference) {
            return null
        }

        return when (reference) {
            // For most constructions the element could be used instead
            is KaFirArrayAccessReference,
            is KaFirCollectionLiteralReference,
            is KaFirConstructorDelegationReference,
            is KaFirDestructuringDeclarationReference,
            is KaFirForLoopInReference,
            is KaFirPropertyDelegationMethodsReference,
            is KaFirSimpleNameReference,
            is KaFirKDocReference,
                -> tryResolveSymbolsForReferenceViaElement(reference)

            is KaFirDefaultAnnotationArgumentReference -> tryResolveSymbolsForDefaultAnnotationArgumentReference(reference)
            is KaFirInvokeFunctionReference -> tryResolveSymbolsForInvokeReference(reference)
        }
    }

    /**
     * Some elements require special adjusting on psi or fir level:
     *
     * - For [KtDestructuringDeclarationEntry], [getOrBuildFir] returns [FirProperty] (a declaration).
     *   The actual resolution target is in [FirProperty.initializer] (e.g., [FirComponentCall] or [FirErrorExpression]).
     *
     * - For [KtPropertyDelegate], [getOrBuildFir] should be called on the property to handle the type specially.
     *   The actual resolution target is in [FirProperty.delegate] (e.g., [FirFunctionCall]), which conflicts with the regular logic.
     */
    private fun KtElement.getOrBuildFirWithAdjustments(): FirElement? = when (this) {
        is KtPropertyDelegate -> (parent as? KtElement)?.getOrBuildFir(resolutionFacade)
        else -> when (val fir = getOrBuildFir(resolutionFacade)) {
            is FirProperty if this is KtDestructuringDeclarationEntry -> fir.initializer
            else -> fir
        }
    }

    private fun resolveSymbol(psi: KtElement): KaSymbolResolutionAttempt? = when (psi) {
        is KDocName -> resolveKDocName(psi)
        is KtNameReferenceExpression if psi.parent is KtValueArgumentName -> {
            getSymbolsByNameArgumentExpression(psi, analysisSession, firSymbolBuilder).ifNotEmpty(::KaBaseSymbolResolutionSuccess)
        }

        else -> psi.getOrBuildFirWithAdjustments()?.unwrapSafeCall()?.toKaSymbolResolutionAttempt(psi)
    }

    private fun resolveKDocName(psi: KDocName): KaSymbolResolutionAttempt? {
        val fullFqName = generateSequence(psi) { it.parent as? KDocName }.last().getQualifiedNameAsFqName()
        val selectedFqName = psi.getQualifiedNameAsFqName()
        val containedTagSectionIfSubject = psi.getTagIfSubject()?.knownTag

        val symbols = KDocReferenceResolver.resolveKdocFqName(
            analysisSession = analysisSession,
            selectedFqName = selectedFqName,
            fullFqName = fullFqName,
            contextElement = psi,
            containedTagSectionIfSubject = containedTagSectionIfSubject,
        )

        if (symbols.isEmpty()) return null
        return KaBaseSymbolResolutionSuccess(backingSymbols = symbols.toList())
    }

    private fun FirElement.toKaSymbolResolutionAttempt(psi: KtElement): KaSymbolResolutionAttempt? = when (this) {
        is FirResolvedTypeRef if psi is KtSimpleNameExpression -> toKaSymbolResolutionAttempt(psi)
        is FirReference -> toKaSymbolResolutionAttempt(psi)

        // IMPORTANT: all branches above must handle `FirDiagnosticHolder` manually
        is FirDiagnosticHolder -> toKaSymbolResolutionError()
        is FirResolvedTypeRef if psi is KtFunctionType -> toKaSymbolResolutionAttemptForFunctionType()
        is FirResolvable -> toKaSymbolResolutionAttempt(psi)
        is FirReturnExpression -> toKaSymbolResolutionAttempt()
        is FirTypeParameter -> toKaSymbolResolutionAttempt()
        is FirResolvedReifiedParameterReference -> toKaSymbolResolutionAttempt()
        is FirVariableAssignment -> lValue.unwrapExpression().toKaSymbolResolutionAttempt(psi)
        is FirSmartCastExpression -> originalExpression.toKaSymbolResolutionAttempt(psi)
        is FirResolvedQualifier if psi is KtSimpleNameExpression -> toKaSymbolResolutionAttempt(psi)
        is FirPackageDirective if psi is KtSimpleNameExpression -> toKaSymbolResolutionAttempt(psi)
        is FirResolvedImport if psi is KtSimpleNameExpression -> toKaSymbolResolutionAttempt(psi)
        else -> null
    }

    override fun KtReference.resolveToSymbols(): Collection<KaSymbol> = withPsiValidityAssertion(element) {
        return doResolveToSymbols(this)
    }

    private fun doResolveToSymbols(reference: KtReference): Collection<KaSymbol> {
        checkWithAttachment(
            reference is KaFirReference,
            { "${reference::class.simpleName} is not extends ${KaFirReference::class.simpleName}" },
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

    override fun performCallCandidatesCollection(psi: KtElement): List<KaCallCandidate> = wrapError(psi) {
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
        val firSymbolToBuild = when (this) {
            is FirSuperReference -> {
                val resolvedTypeRef = superTypeRef as? FirResolvedTypeRef ?: return null
                resolvedTypeRef.toRegularClassSymbol(analysisSession.firSession)
            }

            else -> when (val symbol = symbol) {
                is FirReceiverParameterSymbol if (psi is KtLabelReferenceExpression || symbol.fir is FirScriptReceiverParameter) -> {
                    // Label references should refer to the containing declaration symbol (not the receiver parameter symbol)

                    // Probably the workaround for a script receiver parameter should be dropped
                    // as soon as `KaScriptSymbol` API will be properly designed KT-76360
                    // (currently we don't have a dedicated KaSymbol for script receiver parameter)
                    symbol.containingDeclarationSymbol
                }

                is FirNamedFunctionSymbol if psi is KtNameReferenceExpression && symbol.name == OperatorNameConventions.INVOKE -> {
                    invokeFunctionReceiver(psi)?.let { return it }
                    symbol
                }

                else -> symbol
            }
        }

        if (this is FirDiagnosticHolder) {
            return toKaSymbolResolutionError()
        }

        val symbol = when (val symbol = firSymbolToBuild?.buildSymbol(firSymbolBuilder)) {
            is KaConstructorSymbol if (psi is KtNameReferenceExpression || psi is KtEnumEntrySuperclassReferenceExpression) -> with(analysisSession) {
                // Callee reference for a constructor call is supposed to refer to the class
                // while the entire call refers to the constructor.
                // `KaSymbol` instead of `FirSymbol` is checked intentionally to properly support
                // type-aliased constructors
                symbol.containingDeclaration
            }

            else -> symbol
        } ?: return null

        return KaBaseSymbolResolutionSuccess(backingSymbol = symbol)
    }

    /**
     * [KtNameReferenceExpression] maps directly to the invoke function, so the corresponding [KtCallExpression]
     * has to be checked to get the real callee
     *
     * @see getContainingCallExpressionForCalleeExpression
     */
    private fun invokeFunctionReceiver(psi: KtNameReferenceExpression): KaSymbolResolutionAttempt? {
        val callExpression = psi.getContainingCallExpressionForCalleeExpression() ?: return null
        val implicitInvokeCall = callExpression.getOrBuildFir(analysisSession.resolutionFacade)
            ?.unwrapSafeCall() as? FirImplicitInvokeCall

        return implicitInvokeCall?.explicitReceiver?.toKaSymbolResolutionAttempt(psi)
    }

    private fun FirResolvedQualifier.toKaSymbolResolutionAttempt(psi: KtSimpleNameExpression): KaSymbolResolutionAttempt? {
        return getSymbolsForResolvedQualifier(
            fir = this,
            expression = psi,
            session = analysisSession.firSession,
            symbolBuilder = firSymbolBuilder,
        ).ifNotEmpty(::KaBaseSymbolResolutionSuccess)
    }

    @Suppress("UnusedReceiverParameter")
    private fun FirPackageDirective.toKaSymbolResolutionAttempt(psi: KtSimpleNameExpression): KaSymbolResolutionAttempt? {
        val packageFqName = getQualifierSelected(psi, forQualifiedType = false)
        return firSymbolBuilder.createPackageSymbolIfOneExists(packageFqName)?.let(::KaBaseSymbolResolutionSuccess)
    }

    private fun FirTypeParameter.toKaSymbolResolutionAttempt(): KaSymbolResolutionAttempt {
        return KaBaseSymbolResolutionSuccess(firSymbolBuilder.buildSymbol(symbol))
    }

    private fun FirResolvedReifiedParameterReference.toKaSymbolResolutionAttempt(): KaSymbolResolutionAttempt {
        return KaBaseSymbolResolutionSuccess(firSymbolBuilder.buildSymbol(symbol))
    }

    private fun FirResolvedImport.toKaSymbolResolutionAttempt(psi: KtSimpleNameExpression): KaSymbolResolutionAttempt? {
        return getSymbolsByResolvedImport(
            expression = psi,
            builder = firSymbolBuilder,
            fir = this,
            session = analysisSession.firSession,
        ).ifNotEmpty(::KaBaseSymbolResolutionSuccess)
    }

    private fun FirResolvedTypeRef.toKaSymbolResolutionAttemptForFunctionType(): KaSymbolResolutionAttempt? {
        val symbol = toTargetSymbol(analysisSession.firSession, firSymbolBuilder) ?: return null
        return KaBaseSymbolResolutionSuccess(backingSymbol = symbol)
    }

    private fun FirResolvedTypeRef.toKaSymbolResolutionAttempt(psi: KtSimpleNameExpression): KaSymbolResolutionAttempt? {
        val resolvedTypeSymbols = getSymbolsForResolvedTypeRef(
            expression = psi,
            fir = this,
            session = analysisSession.firSession,
            symbolBuilder = firSymbolBuilder,
        )

        val resolutionError = (this as? FirDiagnosticHolder)?.toKaSymbolResolutionError()?.let { resolutionError ->
            val name = psi.getReferencedNameAsName()
            KaBaseSymbolResolutionError(
                backingDiagnostic = resolutionError.diagnostic,
                // TODO(KT-85949): replace filtering with a proper error/symbols once the issue is fixed.
                // For now it is used to get rid of unrelated classifiers from the result.
                // No need to check packages since they cannot be candidates
                backingCandidateSymbols = resolutionError.candidateSymbols.filter { it is KaNamedSymbol && it.name == name },
            )
        }

        // Resolved symbols might properly detect usages of nested elements,
        // but at the same time, if they found error symbols, the error result has to be preserved
        val errorCandidates = resolutionError?.candidateSymbols
        if (errorCandidates != null &&
            errorCandidates.size == resolvedTypeSymbols.size &&
            errorCandidates.toHashSet().containsAll(resolvedTypeSymbols)
        ) {
            return resolutionError
        }

        return resolvedTypeSymbols.ifNotEmpty(::KaBaseSymbolResolutionSuccess) ?: resolutionError
    }

    private fun FirDiagnosticHolder.toKaSymbolResolutionError(): KaSymbolResolutionError {
        val candidates = if (this is FirNamedReference) {
            getCandidateSymbols()
        } else {
            diagnostic.getCandidateSymbols()
        }

        return KaBaseSymbolResolutionError(
            backingDiagnostic = createKaDiagnostic(),
            backingCandidateSymbols = candidates.map(firSymbolBuilder::buildSymbol),
        )
    }

    private fun FirReturnExpression.toKaSymbolResolutionAttempt(): KaSymbolResolutionAttempt {
        return when (val firFunctionSymbol = target.labeledElement.symbol) {
            is FirErrorFunctionSymbol -> {
                val diagnostic = firFunctionSymbol.fir.createKaDiagnostic()
                KaBaseSymbolResolutionError(backingCandidateSymbols = emptyList(), backingDiagnostic = diagnostic)
            }

            else -> {
                val kaSymbol = firFunctionSymbol.buildSymbol(firSymbolBuilder)
                KaBaseSymbolResolutionSuccess(kaSymbol)
            }
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

        handleMissedConstructorCall(this, psi)?.let { return it }

        if (this is FirImplicitInvokeCall) {

            // If we have a PSI expression like `Foo.Bar.Baz()` and try to resolve `Bar` part,
            // and the only FIR that we have for that PSI is an implicit invoke call, that means that
            // `Foo.Bar` is definitely not a property access - otherwise it would have had its own FIR.
            // So, it does not make sense to try to resolve such parts of qualifiers as KaCallResolutionSuccess
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
        ): KaCallResolutionError where T : FirNamedReference, T : FirDiagnosticHolder = transformErrorReference(
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

            is FirSafeCallExpression -> unwrapSelector(psi).toKaResolutionAttempt(
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
                    KaBaseCallResolutionError(
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
     * Expressions like `s?.itself["1"]` where [KtSafeQualifiedExpression] is `s?.itself` are wrapped into something like `s?.{ $subj$.itself.get("1") }`,
     * so we need to extract `itself` from `$subj$.itself` receiver.
     * But! Expressions like `s?.itself("1")` where [KtSafeQualifiedExpression] is the whole expression might be wrapped the same way.
     * For instance, it is the case for an implicit invoke. It could be wrapped into something like `s?.{ $subj$.itself.invoke("1") }` as well,
     * but it has to be resolved into the call since the expression is call
     */
    private fun FirSafeCallExpression.unwrapSelector(psi: KtElement): FirElement {
        val selector = selector
        if (psi !is KtSafeQualifiedExpression || psi.selectorExpression is KtCallExpression || selector !is FirQualifiedAccessExpression) {
            return selector
        }

        val nonDefaultReceiver = selector.explicitReceiver?.takeUnless { it is FirCheckedSafeCallSubject }
        return nonDefaultReceiver ?: selector
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
    private fun handleMissedConstructorCall(fir: FirElement, psi: KtElement): KaCallResolutionError? {
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
                analysisSession.firSession.languageVersionSettings.supportsFeature(LanguageFeature.ForbidUselessTypeArgumentsIn25) -> {}
                else -> return null
            }
        }

        val constructors = fir.findQualifierConstructors()
        val calls = fir.toKaCalls(constructors)
        return KaBaseCallResolutionError(
            backedDiagnostic = inapplicableCandidateDiagnostic(),
            backingCandidateCalls = calls,
        )
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
    ): KaSingleOrMultiCall? = createKaCallResolutionAttempt(
        psi = psi,
        fir = fir,
        calleeReference = fir.calleeReference,
        candidate = candidate,
        resolveFragmentOfCall = resolveFragmentOfCall,
    )?.successfulCall

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
        )?.let(::KaBaseCallResolutionSuccess)
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
    ): KaSingleCall<*, *>? {
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

                    dispatchReceiverValue = KaBaseExplicitReceiverValue(
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

            // In regular invoke functions (explicitly declared operator invoke functions) context arguments are available on the fir call,
            // while for functional types they explicitly passed as regular arguments
            val adjustedContextArguments = contextArguments.ifEmpty {
                (fir as? FirFunctionCall)?.arguments?.take(argumentsContextParameterCount).orEmpty()
            }

            return KaBasePartiallyAppliedSymbol(
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
                    backingIsContextSensitive = calleeReference?.isContextSensitive == true,
                )
            }

            is FirCallableReferenceAccess -> KaBaseCallableReferenceCall(
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
                    KaBaseSimpleVariableAccessCall(
                        backingPartiallyAppliedSymbol = partiallyAppliedSymbol as KaPartiallyAppliedVariableSymbol<KaVariableSymbol>,
                        backingTypeArgumentsMapping = typeArgumentsMapping,
                        backingKind = KaBaseVariableReadAccess,
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
        val variableAccessCall = KaBaseSimpleVariableAccessCall(
            backingPartiallyAppliedSymbol = variableSymbol,
            backingTypeArgumentsMapping = typeArgumentsMapping,
            backingKind = KaBaseVariableReadAccess,
            backingIsContextSensitive = fir.calleeReference?.isContextSensitive == true,
        )

        if (resolveFragmentOfCall) {
            return KaBaseCallResolutionSuccess(backingCall = variableAccessCall)
        }

        // Extract operation call
        val firOperationCall = fir.rValue as? FirFunctionCall
            ?: getInitializerOfReferencedLocalVariable(fir.rValue) ?: return null


        val operationError = findErrorCall(firOperationCall, psi)
        val operationAttempt: KaSingleCallResolutionAttempt
        val compoundOperation: KaCompoundOperation?

        if (operationError != null) {
            operationAttempt = operationError
            compoundOperation = null
        } else {
            val operationCall = buildOperationCallForCompoundVariableAccess(firOperationCall, accessExpression, rhsExpression)
                ?: return null
            operationAttempt = KaBaseCallResolutionSuccess(backingCall = operationCall)
            compoundOperation = compoundOperationProvider(operationCall)
        }

        val variableAttempt = KaBaseCallResolutionSuccess(backingCall = variableAccessCall)
        return KaBaseCompoundVariableAccessCallResolutionAttempt(
            backingCompoundOperation = compoundOperation,
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
    ): KaCallResolutionError {
        val diagnostic = diagnosticHolder.diagnostic
        val kaDiagnostic = diagnosticHolder.createKaDiagnostic()

        if (diagnostic is ConeHiddenCandidateError) {
            return KaBaseCallResolutionError(
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
                    ) as? KaCallResolutionSuccess

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
            ) as? KaCallResolutionSuccess

            listOfNotNull(attempt?.call)
        }

        return KaBaseCallResolutionError(
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

        @OptIn(SymbolInternals::class)
        val firIteratorCall = iteratorPropertySymbol.fir
            .initializer
            ?.asFunctionOperatorCall(KtFakeSourceElementKind.DesugaredForLoop)
            ?: return null

        @OptIn(SymbolInternals::class)
        val firNextCall = (firLoop.block.statements.firstOrNull() as? FirProperty)
            ?.initializer
            ?.asFunctionOperatorCall(KtFakeSourceElementKind.DesugaredForLoop)
            ?: return null

        val iteratorAttempt = resolveSingleSubCall(firIteratorCall, psi)
        val hasNextAttempt = resolveSingleSubCall(firHasNextCall, psi)
        val nextAttempt = resolveSingleSubCall(firNextCall, psi)

        return KaBaseForLoopCallResolutionAttempt(
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

        return KaBaseDelegatedPropertyCallResolutionAttempt(
            backingValueGetterCallAttempt = getterAttempt,
            backingValueSetterCallAttempt = setterAttempt,
            backingProvideDelegateCallAttempt = provideDelegateAttempt,
        )
    }

    private fun findErrorCall(
        call: FirFunctionCall,
        psi: KtElement,
    ): KaCallResolutionError? = when (val ref = call.calleeReference) {
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
     * Resolves a [FirFunctionCall] into a [KaSingleCallResolutionAttempt].
     * If the call has an error, returns [KaCallResolutionError]; otherwise builds a [KaCallResolutionSuccess].
     */
    private fun resolveSingleSubCall(call: FirFunctionCall, psi: KtElement): KaSingleCallResolutionAttempt {
        findErrorCall(call, psi)?.let { return it }

        return when (val kaCall = buildNamedFunctionCall(call)) {
            null -> KaBaseCallResolutionError(
                backedDiagnostic = KaNonBoundToPsiErrorDiagnostic(
                    factoryName = FirErrors.OTHER_ERROR.name,
                    defaultMessage = "Failed to build call",
                    token = token,
                ),
                backingCandidateCalls = emptyList(),
            )

            else -> KaBaseCallResolutionSuccess(backingCall = kaCall)
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
                            compoundOperationProvider = { KaBaseCompoundAssignOperation(it, compoundAssignKind, rightOperandPsi) },
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
                            compoundOperationProvider = { KaBaseCompoundUnaryOperation(it, incOrDecOperationKind, precedence) },
                        )
                    },
                    compoundOperationProvider = { KaBaseCompoundUnaryOperation(it, incOrDecOperationKind, precedence) },
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
        val getterAttempt: KaSingleCallResolutionAttempt
        if (getterError != null) {
            getterAttempt = getterError
        } else {
            val getPartiallyAppliedSymbol = firGetCall.toPartiallyAppliedSymbol(arrayExpression) ?: return null
            val getArgumentMapping = indexExpressions.zip(getPartiallyAppliedSymbol.signature.valueParameters).toMap()
            val getCall = KaBaseSimpleFunctionCall(
                backingPartiallyAppliedSymbol = getPartiallyAppliedSymbol,
                backingArgumentMapping = getArgumentMapping,
                backingTypeArgumentsMapping = firCall
                    .toFirTypeArgumentsMapping(symbol = getPartiallyAppliedSymbol.symbol.firSymbol)
                    .asKaTypeParametersMapping(),
            ).let {
                @Suppress("UNCHECKED_CAST")
                it as KaFunctionCall<KaNamedFunctionSymbol>
            }

            getterAttempt = KaBaseCallResolutionSuccess(backingCall = getCall)
        }

        if (resolveFragmentOfCall) {
            return getterAttempt
        }

        // Build operation call or error
        val operationError = findErrorCall(firOperationCall, psi)
        val operationAttempt: KaSingleCallResolutionAttempt
        val compoundOperation: KaCompoundOperation?
        if (operationError != null) {
            operationAttempt = operationError
            compoundOperation = null
        } else {
            // The explicit receiver in this case is a synthetic FirFunctionCall to `get`, which does not have a corresponding PSI. So
            // we use the `lhsArrayAccessExpression` as the supplement.
            val operationPartiallyAppliedSymbol = firOperationCall.toPartiallyAppliedSymbol(lhsArrayAccessExpression) ?: return null
            val operationArgumentsMapping = listOfNotNull(lhsArrayAccessExpression, rhsExpression)
                .zip(operationPartiallyAppliedSymbol.signature.valueParameters)
                .toMap()
            val operationCall = KaBaseSimpleFunctionCall(
                backingPartiallyAppliedSymbol = operationPartiallyAppliedSymbol,
                backingArgumentMapping = operationArgumentsMapping,
                backingTypeArgumentsMapping = firCall
                    .toFirTypeArgumentsMapping(symbol = operationPartiallyAppliedSymbol.symbol.firSymbol)
                    .asKaTypeParametersMapping(),
            ).let {
                @Suppress("UNCHECKED_CAST")
                it as KaFunctionCall<KaNamedFunctionSymbol>
            }

            operationAttempt = KaBaseCallResolutionSuccess(backingCall = operationCall)
            compoundOperation = compoundOperationProvider(operationCall)
        }

        // Build setter call or error
        val setterError = findErrorCall(firCall, psi)
        val setterAttempt: KaSingleCallResolutionAttempt
        if (setterError != null) {
            setterAttempt = setterError
        } else {
            val setPartiallyAppliedSymbol = firCall.toPartiallyAppliedSymbol(arrayExpression) ?: return null
            val setArgumentsMapping = mapOf(indexExpressions.last() to setPartiallyAppliedSymbol.signature.valueParameters.last())
            val setCall = KaBaseSimpleFunctionCall(
                backingPartiallyAppliedSymbol = setPartiallyAppliedSymbol,
                backingArgumentMapping = setArgumentsMapping,
                backingTypeArgumentsMapping = firCall
                    .toFirTypeArgumentsMapping(symbol = setPartiallyAppliedSymbol.symbol.firSymbol)
                    .asKaTypeParametersMapping(),
            ).let {
                @Suppress("UNCHECKED_CAST")
                it as KaFunctionCall<KaNamedFunctionSymbol>
            }

            setterAttempt = KaBaseCallResolutionSuccess(backingCall = setCall)
        }

        return KaBaseCompoundArrayAccessCallResolutionAttempt(
            backingCompoundOperation = compoundOperation,
            backingIndexArguments = indexExpressions,
            backingGetterCallAttempt = getterAttempt,
            backingOperationCallAttempt = operationAttempt,
            backingSetterCallAttempt = setterAttempt,
        )
    }

    @OptIn(SymbolInternals::class)
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
            is FirSafeCallExpression -> unwrapSelector(psi).collectCallCandidates(
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
        is KaCallResolutionSuccess -> listOf(KaBaseApplicableCallCandidate(backingCandidate = call, backingIsInBestCandidates = true))
        is KaCallResolutionError -> candidateCalls.map {
            KaBaseInapplicableCallCandidate(
                backingCandidate = it,
                backingIsInBestCandidates = true,
                backingDiagnostic = diagnostic,
            )
        }

        is KaMultiCallResolutionAttempt -> fold(
            onSuccess = { listOf(KaBaseApplicableCallCandidate(backingCandidate = it, backingIsInBestCandidates = true)) },
            onFailure = { attempts -> attempts.flatMap { it.toKaCallCandidates() } },
        )

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
        val kaDiagnostic = resolvable.source?.let { diagnostic.asKaDiagnostic(it) }
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
            is FirFunctionTypeConversionExpression ->
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

    private fun FirDiagnosticHolder.createKaDiagnostic(): KaDiagnostic {
        return source?.let { diagnostic.asKaDiagnostic(it) }
            ?: KaNonBoundToPsiErrorDiagnostic(factoryName = FirErrors.OTHER_ERROR.name, diagnostic.reason, token)
    }
}

private val FirReference.isContextSensitive: Boolean
    get() = this is FirResolvedNamedReference && resolvedSymbolOrigin == FirResolvedSymbolOrigin.ContextSensitive
