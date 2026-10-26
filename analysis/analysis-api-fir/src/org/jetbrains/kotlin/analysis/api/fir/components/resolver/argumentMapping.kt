/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.resolver

import org.jetbrains.kotlin.analysis.api.fir.components.KaFirResolver
import org.jetbrains.kotlin.analysis.api.fir.components.analysisSession
import org.jetbrains.kotlin.analysis.api.signatures.KaFunctionSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLabeledExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.topParenthesizedParentOrMe
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal fun FirCall.createArgumentMapping(signatureOfCallee: KaFunctionSignature<*>): Map<KtExpression, KaVariableSignature<KaParameterSymbol>> {
    return resolvedArgumentMappingIncludingContextArguments?.entries.createArgumentMapping(signatureOfCallee)
}

internal fun Collection<MutableMap.MutableEntry<FirExpression, FirValueParameter>>?.createArgumentMapping(
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
    this.forEach { [firExpression, firValueParameter] ->
        val parameterSymbol = paramSignatureByName[firValueParameter.name] ?: return@forEach
        mapArgumentExpressionToParameter(firExpression, parameterSymbol, argumentMapping)
    }

    return argumentMapping
}

context(resolver: KaFirResolver)
internal fun FirCollectionLiteral.createArgumentMapping(
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
            whenSubject?.findSourceKtExpressionForCallArgument()
        // FirBlock is a fake container for desugared expressions like `++index` or `++list[0]`
        is FirBlock -> psi as? KtExpression
        else -> realPsi as? KtExpression
    }?.topParenthesizedParentOrMe()
}
