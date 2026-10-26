/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.resolver

import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostic
import org.jetbrains.kotlin.analysis.api.fir.components.*
import org.jetbrains.kotlin.analysis.api.fir.getCandidateSymbols
import org.jetbrains.kotlin.analysis.api.fir.isInvokeFunction
import org.jetbrains.kotlin.analysis.api.fir.resolution.*
import org.jetbrains.kotlin.analysis.api.fir.utils.KaNonBoundToPsiErrorDiagnostic
import org.jetbrains.kotlin.analysis.api.fir.utils.withSymbolAttachment
import org.jetbrains.kotlin.analysis.api.impl.base.util.withPsiEntry
import org.jetbrains.kotlin.analysis.api.projectStructure.kaModule
import org.jetbrains.kotlin.analysis.api.resolution.*
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaFunctionSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableSignature
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnosticHolder
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.stages.TypeArgumentMapping
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeDiagnosticWithCandidates
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeHiddenCandidateError
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.transformers.unwrapAtoms
import org.jetbrains.kotlin.fir.scopes.getDeclaredConstructors
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.fir.utils.exceptions.withFirSymbolEntry
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getPossiblyQualifiedCallExpression
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.jetbrains.kotlin.resolve.calls.inference.buildCurrentSubstitutor
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.toKtPsiSourceElement
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.kotlin.utils.exceptions.checkWithAttachment
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

context(resolver: KaFirResolver)
internal fun FirElement.toKaResolutionAttempt(
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
        val signature = resolver.stringPlusSymbol?.toKaSignature() ?: return null
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
context(component: KaFirSessionComponent)
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

context(component: KaFirSessionComponent)
internal fun inapplicableCandidateDiagnostic(): KaDiagnostic {
    return KaNonBoundToPsiErrorDiagnostic(factoryName = FirErrors.OTHER_ERROR.name, "Inapplicable candidate", component.token)
}

context(component: KaFirSessionComponent)
internal fun createKaCall(
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

context(component: KaFirSessionComponent)
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

context(component: KaFirSessionComponent)
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
context(component: KaFirSessionComponent)
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

context(component: KaFirSessionComponent)
internal fun transformErrorReference(
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

context(component: KaFirSessionComponent)
internal fun findErrorCall(
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

context(component: KaFirSessionComponent)
internal fun FirResolvedQualifier.findQualifierConstructors(): List<FirConstructorSymbol> {
    val classSymbol = this.qualifierSymbol?.fullyExpandedClass(analysisSession.firSession) ?: return emptyList()
    return classSymbol.unsubstitutedScope(
        analysisSession.firSession,
        analysisSession.getScopeSessionFor(analysisSession.firSession),
        withForcedTypeCalculator = true,
        memberRequiredPhase = null,
    ).getDeclaredConstructors()
}

context(component: KaFirSessionComponent)
internal fun FirResolvedQualifier.toKaCalls(constructors: List<FirConstructorSymbol>): List<KaFunctionCall<*>> =
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

internal val FirResolvable.calleeOrCandidateName: Name?
    get() = this.calleeReference.calleeOrCandidateName

internal val FirReference.calleeOrCandidateName: Name?
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
