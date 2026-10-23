/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.resolver

import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostic
import org.jetbrains.kotlin.analysis.api.fir.components.*
import org.jetbrains.kotlin.analysis.api.fir.resolution.*
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirArrayOfSymbolProvider.arrayOfSymbol
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirEmptySubstitutor
import org.jetbrains.kotlin.analysis.api.fir.utils.KaNonBoundToPsiErrorDiagnostic
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.processEqualsFunctions
import org.jetbrains.kotlin.analysis.api.resolution.KaCallResolutionAttempt
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.typeParameters
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirCollectionLiteral
import org.jetbrains.kotlin.fir.expressions.FirEqualityOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.toArrayOfFactoryName
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.arrayElementType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiUtil.deparenthesize
import org.jetbrains.kotlin.resolve.ArrayFqNames

context(component: KaFirSessionComponent)
private fun FirCollectionLiteral.toTypeArgumentsMapping(symbol: KaDeclarationSymbol): Map<KaTypeParameterSymbol, KaType> {
    val elementType = resolvedType.arrayElementType()?.asKaType() ?: return emptyMap()
    val typeParameter = symbol.typeParameters.singleOrNull() ?: return emptyMap()
    return mapOf(typeParameter to elementType)
}

context(component: KaFirSessionComponent)
private val unresolvedArrayOfDiagnostic: KaDiagnostic
    get() = KaNonBoundToPsiErrorDiagnostic(
        factoryName = FirErrors.OTHER_ERROR.name,
        defaultMessage = "type of arrayOf call is not resolved",
        token = component.token
    )

context(component: KaFirSessionComponent)
internal fun FirCollectionLiteral.toKaResolutionAttempt(): KaCallResolutionAttempt? {
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

context(component: KaFirSessionComponent)
private fun FirCollectionLiteral.createSubstitutorFromTypeArguments(arrayOfSymbol: KaNamedFunctionSymbol): KaSubstitutor {
    val firSymbol = arrayOfSymbol.firSymbol
    // No type parameter means this is an arrayOf call of primitives, in which case there is no type arguments
    val typeParameter = firSymbol.fir.typeParameters.singleOrNull() ?: return KaFirEmptySubstitutor(component.token)

    val elementType = resolvedType.arrayElementType() ?: return KaFirEmptySubstitutor(component.token)
    val coneSubstitutor = substitutorByMap(mapOf(typeParameter.symbol to elementType), rootModuleSession)
    return firSymbolBuilder.typeBuilder.buildSubstitutor(coneSubstitutor)
}

context(resolver: KaFirResolver)
internal fun FirEqualityOperatorCall.toKaResolutionAttempt(psi: KtElement): KaCallResolutionAttempt? {
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

context(resolver: KaFirResolver)
private fun FirEqualityOperatorCall.getEqualsSymbol(): FirNamedFunctionSymbol? {
    var equalsSymbol: FirNamedFunctionSymbol? = null
    processEqualsFunctions(analysisSession.firSession, analysisSession) {
        if (equalsSymbol != null) return@processEqualsFunctions
        equalsSymbol = it
    }

    return equalsSymbol ?: resolver.equalsSymbolInAny
}
