/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirNamedArgumentExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildVarargArgumentsExpression
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.ConstantValueKind

internal inline var FirExpression.resultType: ConeKotlinType?
    get() = coneTypeOrNull
    set(type) {
        replaceConeTypeOrNull(type)
    }

internal fun remapArgumentsWithVararg(
    varargParameter: FirValueParameter,
    varargArrayType: ConeKotlinType,
    argumentMapping: LinkedHashMap<FirExpression, FirValueParameter>
): LinkedHashMap<FirExpression, FirValueParameter> {
    // Create a FirVarargArgumentExpression for the vararg arguments.
    // The order of arguments in the mapping must be preserved for FIR2IR, hence we have to find where the vararg arguments end.
    // FIR2IR uses the mapping order to determine if arguments need to be reordered.
    val varargParameterTypeRef = varargParameter.returnTypeRef
    val varargElementType = varargArrayType.arrayElementType()?.approximateIntegerLiteralType()
    val argumentList = argumentMapping.keys.toList()
    var indexAfterVarargs = argumentList.size
    val newArgumentMapping = linkedMapOf<FirExpression, FirValueParameter>()
    val varargArgument = buildVarargArgumentsExpression {
        // TODO: ideally we should use here a source from the use-site and not from the declaration-site, KT-59682
        this.varargElementType = varargParameterTypeRef.withReplacedConeType(varargElementType, KtFakeSourceElementKind.VarargArgument)
        this.coneTypeOrNull = varargArrayType
        var startOffset = Int.MAX_VALUE
        var endOffset = 0
        var firstVarargElementSource: KtSourceElement? = null

        for ((i, arg) in argumentList.withIndex()) {
            val valueParameter = argumentMapping.getValue(arg)
            // Collect arguments if `arg` is a vararg argument of interest or other vararg arguments.
            if (valueParameter == varargParameter ||
                // NB: don't pull out of named arguments.
                (valueParameter.isVararg && arg !is FirNamedArgumentExpression)
            ) {
                arguments += arg
                startOffset = minOf(startOffset, arg.source?.startOffset ?: Int.MAX_VALUE)
                endOffset = maxOf(endOffset, arg.source?.endOffset ?: 0)
                if (firstVarargElementSource == null) firstVarargElementSource = arg.source
            } else if (arguments.isEmpty()) {
                // `arg` is BEFORE the vararg arguments.
                newArgumentMapping[arg] = valueParameter
            } else {
                // `arg` is AFTER the vararg arguments.
                indexAfterVarargs = i
                break
            }
        }

        source = firstVarargElementSource?.fakeElement(KtFakeSourceElementKind.VarargArgument, startOffset, endOffset)
    }
    newArgumentMapping[varargArgument] = varargParameter

    // Add mapping for arguments after the vararg arguments, if any.
    for (i in indexAfterVarargs until argumentList.size) {
        val arg = argumentList[i]
        newArgumentMapping[arg] = argumentMapping.getValue(arg)
    }
    return newArgumentMapping
}

fun FirBlock.writeResultType(session: FirSession) {
    val resultExpression = when (val statement = statements.lastOrNull()) {
        is FirExpression -> statement
        else -> null
    }
    resultType = if (resultExpression == null) {
        session.builtinTypes.unitType.type
    } else {
        resultExpression.resultType ?: ConeErrorType(ConeSimpleDiagnostic("No type for block", DiagnosticKind.InferenceError))
    }
}

fun ConstantValueKind<*>.expectedConeType(session: FirSession): ConeKotlinType {
    fun constructLiteralType(classId: ClassId, isNullable: Boolean = false): ConeKotlinType {
        val symbol = session.symbolProvider.getClassLikeSymbolByClassId(classId)
            ?: return ConeErrorType(ConeSimpleDiagnostic("Missing stdlib class: $classId", DiagnosticKind.MissingStdlibClass))
        return symbol.toLookupTag().constructClassType(ConeTypeProjection.EMPTY_ARRAY, isNullable)
    }
    return when (this) {
        ConstantValueKind.Null -> session.builtinTypes.nullableNothingType.type
        ConstantValueKind.Boolean -> session.builtinTypes.booleanType.type
        ConstantValueKind.Char -> constructLiteralType(StandardClassIds.Char)
        ConstantValueKind.Byte -> constructLiteralType(StandardClassIds.Byte)
        ConstantValueKind.Short -> constructLiteralType(StandardClassIds.Short)
        ConstantValueKind.Int -> constructLiteralType(StandardClassIds.Int)
        ConstantValueKind.Long -> constructLiteralType(StandardClassIds.Long)
        ConstantValueKind.String -> constructLiteralType(StandardClassIds.String)
        ConstantValueKind.Float -> constructLiteralType(StandardClassIds.Float)
        ConstantValueKind.Double -> constructLiteralType(StandardClassIds.Double)

        ConstantValueKind.UnsignedByte -> constructLiteralType(StandardClassIds.UByte)
        ConstantValueKind.UnsignedShort -> constructLiteralType(StandardClassIds.UShort)
        ConstantValueKind.UnsignedInt -> constructLiteralType(StandardClassIds.UInt)
        ConstantValueKind.UnsignedLong -> constructLiteralType(StandardClassIds.ULong)

        ConstantValueKind.IntegerLiteral -> constructLiteralType(StandardClassIds.Int)
        ConstantValueKind.UnsignedIntegerLiteral -> constructLiteralType(StandardClassIds.UInt)
        ConstantValueKind.Error -> error("Unexpected error ConstantValueKind")
    }
}
