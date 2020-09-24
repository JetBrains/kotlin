/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildVarargArgumentsExpression
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
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

internal fun remapArgumentsWithVararg(
    varargParameter: FirValueParameter,
    varargArrayType: ConeKotlinType,
    argumentList: FirArgumentList,
    argumentMapping: Map<FirExpression, FirValueParameter>
): Map<FirExpression, FirValueParameter> {
    // Create a FirVarargArgumentExpression for the vararg arguments.
    // The order of arguments in the mapping must be preserved for FIR2IR, hence we have to find where the vararg arguments end.
    // FIR2IR uses the mapping order to determine if arguments need to be reordered.
    val varargParameterTypeRef = varargParameter.returnTypeRef
    val varargElementType = varargArrayType.arrayElementType()
    var indexAfterVarargs = argumentList.arguments.size
    val newArgumentMapping = mutableMapOf<FirExpression, FirValueParameter>()
    val varargArgument = buildVarargArgumentsExpression {
        this.varargElementType = varargParameterTypeRef.withReplacedConeType(varargElementType)
        this.typeRef = varargParameterTypeRef.withReplacedConeType(varargArrayType)
        for ((i, arg) in argumentList.arguments.withIndex()) {
            val valueParameter = argumentMapping[arg] ?: continue
            if (valueParameter.isVararg) {
                // `arg` is a vararg argument.
                arguments += arg
            } else if (arguments.isEmpty()) {
                // `arg` is BEFORE the vararg arguments.
                newArgumentMapping[arg] = valueParameter
            } else {
                // `arg` is AFTER the vararg arguments.
                indexAfterVarargs = i
                break
            }
        }
    }
    newArgumentMapping[varargArgument] = varargParameter

    // Add mapping for arguments after the vararg arguments, if any.
    for (i in indexAfterVarargs until argumentList.arguments.size) {
        val arg = argumentList.arguments[i]
        val valueParameter = argumentMapping[arg] ?: continue
        newArgumentMapping[arg] = valueParameter
    }
    return newArgumentMapping
}

fun FirBlock.writeResultType(session: FirSession) {
    val resultExpression = when (val statement = statements.lastOrNull()) {
        is FirReturnExpression -> statement.result
        is FirExpression -> statement
        else -> null
    }
    resultType = if (resultExpression == null) {
        resultType.resolvedTypeFromPrototype(session.builtinTypes.unitType.type)
    } else {
        val theType = resultExpression.resultType
        if (theType is FirResolvedTypeRef) {
            buildResolvedTypeRef {
                source = theType.source?.fakeElement(FirFakeSourceElementKind.ImplicitTypeRef)
                type = theType.type
                annotations += theType.annotations
            }
        } else {
            buildErrorTypeRef {
                diagnostic = ConeSimpleDiagnostic("No type for block", DiagnosticKind.InferenceError)
            }
        }
    }
}
