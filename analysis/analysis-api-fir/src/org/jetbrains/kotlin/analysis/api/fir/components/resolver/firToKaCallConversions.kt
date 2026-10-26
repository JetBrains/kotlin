/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.resolver

import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostic
import org.jetbrains.kotlin.analysis.api.fir.components.*
import org.jetbrains.kotlin.analysis.api.fir.resolution.KaFirExplicitReceiverValue
import org.jetbrains.kotlin.analysis.api.fir.resolution.KaFirImplicitReceiverValue
import org.jetbrains.kotlin.analysis.api.fir.resolution.KaFirPartiallyAppliedSymbol
import org.jetbrains.kotlin.analysis.api.fir.resolution.KaFirSmartCastedReceiverValue
import org.jetbrains.kotlin.analysis.api.fir.utils.KaNonBoundToPsiErrorDiagnostic
import org.jetbrains.kotlin.analysis.api.resolution.*
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaFunctionSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableSignature
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnosticHolder
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiUtil.deparenthesize
import org.jetbrains.kotlin.psi.KtSafeQualifiedExpression
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.exceptions.checkWithAttachment
import org.jetbrains.kotlin.utils.exceptions.logErrorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

context(component: KaFirSessionComponent)
internal fun FirVariableAssignment.toPartiallyAppliedSymbol(): KaPartiallyAppliedVariableSymbol<KaVariableSymbol>? {
    val variableRef = calleeReference as? FirResolvedNamedReference ?: return null
    val variableSymbol = variableRef.resolvedSymbol as? FirVariableSymbol<*> ?: return null
    val substitutor = unwrapLValue()?.createConeSubstitutorFromTypeArguments(rootModuleSession) ?: return null
    val kaSignature = variableSymbol.toKaSignature()
    return KaFirPartiallyAppliedSymbol(
        backingSignature = kaSignature.substitute(substitutor.toKaSubstitutor()),
        dispatchReceiver = dispatchReceiver?.toKaReceiverValue(),
        extensionReceiver = extensionReceiver?.toKaReceiverValue(),
        contextArguments = contextArguments.toKaContextParameterValues(),
    )
}

context(component: KaFirSessionComponent)
internal fun FirFunctionCall.toPartiallyAppliedSymbol(
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
    return KaFirPartiallyAppliedSymbol(
        backingSignature = kaSignature.substitute(substitutor.toKaSubstitutor()),
        dispatchReceiver = dispatchReceiverValue,
        extensionReceiver = extensionReceiverValue,
        contextArguments = contextArguments.toKaContextParameterValues(),
    )
}

context(component: KaFirSessionComponent)
internal fun List<FirExpression>.toKaContextParameterValues(): List<KaReceiverValue> = mapNotNull { expression ->
    val receiverValue = expression.toKaReceiverValue()
    if (receiverValue == null && expression !is FirErrorExpression) {
        logger<KaFirResolver>().logErrorWithAttachment("Unexpected null receiver value for context argument") {
            withFirEntry("expression", expression)
        }
    }

    receiverValue
}

context(component: KaFirSessionComponent)
internal fun FirExpression.toKaReceiverValue(): KaReceiverValue? {
    return when (this) {
        is FirSmartCastExpression -> {
            val result = originalExpression.toKaReceiverValue()
            if (result != null && isStable) {
                KaFirSmartCastedReceiverValue(result, smartcastType.coneType.asKaType())
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

            KaFirImplicitReceiverValue(symbol, resolvedType.asKaType())
        }

        is FirPropertyAccessExpression if source?.kind is KtFakeSourceElementKind.ImplicitContextParameterArgument -> {
            val firSymbol = calleeReference.symbol ?: return null

            require(firSymbol is FirValueParameterSymbol) { "Unexpected symbol ${firSymbol::class.simpleName}" }
            val symbol = firSymbolBuilder.variableBuilder.buildParameterSymbol(firSymbol)
            KaFirImplicitReceiverValue(symbol, resolvedType.asKaType())
        }

        is FirResolvedQualifier if this.source?.kind is KtFakeSourceElementKind.ImplicitReceiver -> {
            val symbol = this.qualifierSymbol ?: return null
            @OptIn(ResolvedQualifierTypeAccess::class)
            KaFirImplicitReceiverValue(symbol.toKaSymbol(), resolvedType.asKaType())
        }

        else -> {
            val psi = psi
            if (psi !is KtExpression) return null
            psi.toExplicitReceiverValue(resolvedType.asKaType())
        }
    }
}

context(component: KaFirSessionComponent)
internal fun FirCallableSymbol<*>.toKaSignature(): KaCallableSignature<KaCallableSymbol> =
    firSymbolBuilder.callableBuilder.buildCallableSignature(this)

context(component: KaFirSessionComponent)
private fun FirClassLikeSymbol<*>.toKaSymbol(): KaClassLikeSymbol = firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(this)

context(component: KaFirSessionComponent)
internal fun FirNamedFunctionSymbol.toKaSignature(): KaFunctionSignature<KaNamedFunctionSymbol> =
    firSymbolBuilder.functionBuilder.buildNamedFunctionSignature(this)

context(component: KaFirSessionComponent)
internal fun FirVariableSymbol<*>.toKaSignature(): KaVariableSignature<KaVariableSymbol> =
    firSymbolBuilder.variableBuilder.buildVariableLikeSignature(this)

internal fun FirQualifiedAccessExpression.toFirTypeArgumentsMapping(symbol: FirCallableSymbol<*>): Map<FirTypeParameterSymbol, ConeKotlinType> {
    return toFirTypeArgumentsMapping(typeArguments, symbol)
}

internal fun FirResolvedQualifier.toFirTypeArgumentsMapping(symbol: FirCallableSymbol<*>): Map<FirTypeParameterSymbol, ConeKotlinType> {
    return toFirTypeArgumentsMapping(typeArguments, symbol)
}

internal fun FirDelegatedConstructorCall.toFirTypeArgumentsMapping(symbol: FirCallableSymbol<*>): Map<FirTypeParameterSymbol, ConeKotlinType> {
    val typeParameters = symbol.typeParameterSymbols.ifEmpty { return emptyMap() }
    val typeArguments = constructedTypeRef.coneType.typeArguments
    // In all cases, the size of arguments and parameters is the same,
    // so this check exists just to be sure
    if (typeArguments.size != typeParameters.size) return emptyMap()

    return buildMap(typeArguments.size) {
        for ([index, projection] in typeArguments.withIndex()) {
            if (projection !is ConeKotlinType) return emptyMap()
            put(typeParameters[index], projection)
        }
    }
}

/**
 * Maps [typeArguments] to the type parameters of [symbol].
 *
 * If too many type arguments are provided, a mapping is still created. Extra type arguments are simply ignored. If this wasn't the
 * case, the resulting [KaSimpleCallResolutionSuccess] would contain no type arguments at all, which can cause problems later. If too few type arguments are
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

    for ([index, typeParameter] in typeParameters.withIndex()) {
        // After resolution all type arguments should be usual types (not FirPlaceholderProjection)
        val typeArgument = typeArguments[index]
        if (typeArgument !is FirTypeProjectionWithVariance || typeArgument.variance != Variance.INVARIANT) return emptyMap()
        result[typeParameter] = typeArgument.typeRef.coneType
    }

    return result
}

context(component: KaFirSessionComponent)
internal fun Map<FirTypeParameterSymbol, ConeKotlinType>.asKaTypeParametersMapping(): Map<KaTypeParameterSymbol, KaType> {
    return map { [key, value] ->
        firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(key) to value.asKaType()
    }.toMap()
}

context(component: KaFirSessionComponent)
internal fun FirDiagnosticHolder.createKaDiagnostic(): KaDiagnostic {
    return source?.let { diagnostic.asKaDiagnostic(it) }
        ?: KaNonBoundToPsiErrorDiagnostic(factoryName = FirErrors.OTHER_ERROR.name, diagnostic.reason, component.token)
}

private fun KtExpression.toExplicitReceiverValue(type: KaType): KaExplicitReceiverValue =
    KaFirExplicitReceiverValue(expression = this, backingType = type, isSafeNavigation = isReceiverOfKtSafeQualifiedExpression())

private fun KtExpression.isReceiverOfKtSafeQualifiedExpression(): Boolean {
    val safeQualifiedExpression = parentOfType<KtSafeQualifiedExpression>() ?: return false
    return deparenthesize(safeQualifiedExpression.receiverExpression) == deparenthesize(this)
}
