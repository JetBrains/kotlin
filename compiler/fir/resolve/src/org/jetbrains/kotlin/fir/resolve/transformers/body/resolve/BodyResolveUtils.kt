/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildVarargArgumentsExpression
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.renderWithType
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.inference.InferenceComponents
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.scopes.impl.withReplacedConeType
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.arrayElementType
import kotlin.math.min

inline fun <reified T : FirElement> FirBasedSymbol<*>.firUnsafe(): T {
    val fir = this.fir
    require(fir is T) {
        "Not an expected fir element type = ${T::class}, symbol = ${this}, fir = ${fir.renderWithType()}"
    }
    return fir
}

internal inline var FirExpression.resultType: FirTypeRef
    get() = typeRef
    set(type) {
        replaceTypeRef(type)
    }

internal fun inferenceComponents(
    session: FirSession,
    returnTypeCalculator: ReturnTypeCalculator,
    scopeSession: ScopeSession
): InferenceComponents {
    val inferenceContext = session.typeContext
    return InferenceComponents(inferenceContext, session, returnTypeCalculator, scopeSession)
}

internal fun remapArgumentsWithVararg(
    varargParameter: FirValueParameter,
    varargArrayType: ConeKotlinType,
    argumentList: FirArgumentList,
    argumentMapping: Map<FirExpression, FirValueParameter>
): Map<FirExpression, FirValueParameter> {
    // Create a FirVarargArgumentExpression for the vararg arguments
    val varargParameterTypeRef = varargParameter.returnTypeRef
    val varargElementType = varargArrayType.arrayElementType()
    var firstIndex = argumentList.arguments.size
    val newArgumentMapping = mutableMapOf<FirExpression, FirValueParameter>()
    val varargArgument = buildVarargArgumentsExpression {
        this.varargElementType = varargParameterTypeRef.withReplacedConeType(varargElementType)
        this.typeRef = varargParameterTypeRef.withReplacedConeType(varargArrayType)
        for ((i, arg) in argumentList.arguments.withIndex()) {
            val valueParameter = argumentMapping[arg] ?: continue
            if (valueParameter.isVararg) {
                firstIndex = min(firstIndex, i)
                arguments += arg
            } else {
                newArgumentMapping[arg] = valueParameter
            }
        }
    }
    newArgumentMapping[varargArgument] = varargParameter
    return newArgumentMapping
}
