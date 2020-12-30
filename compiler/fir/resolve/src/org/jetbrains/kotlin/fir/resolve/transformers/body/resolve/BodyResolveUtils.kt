/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildVarargArgumentsExpression
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.name.ClassId

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
    argumentMapping: LinkedHashMap<FirExpression, FirValueParameter>
): LinkedHashMap<FirExpression, FirValueParameter> {
    // Create a FirVarargArgumentExpression for the vararg arguments.
    // The order of arguments in the mapping must be preserved for FIR2IR, hence we have to find where the vararg arguments end.
    // FIR2IR uses the mapping order to determine if arguments need to be reordered.
    val varargParameterTypeRef = varargParameter.returnTypeRef
    val varargElementType = varargArrayType.arrayElementType()
    val argumentList = argumentMapping.keys.toList()
    var indexAfterVarargs = argumentList.size
    val newArgumentMapping = linkedMapOf<FirExpression, FirValueParameter>()
    val varargArgument = buildVarargArgumentsExpression {
        this.varargElementType = varargParameterTypeRef.withReplacedConeType(varargElementType)
        this.typeRef = varargParameterTypeRef.withReplacedConeType(varargArrayType)
        for ((i, arg) in argumentList.withIndex()) {
            val valueParameter = argumentMapping.getValue(arg)
            // Collect arguments if `arg` is a vararg argument of interest or other vararg arguments.
            if (valueParameter == varargParameter ||
                // NB: don't pull out of named arguments.
                (valueParameter.isVararg && arg !is FirNamedArgumentExpression)
            ) {
                arguments += arg
                if (this.source == null) {
                    this.source = arg.source?.fakeElement(FirFakeSourceElementKind.VarargArgument)
                }
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
    for (i in indexAfterVarargs until argumentList.size) {
        val arg = argumentList[i]
        newArgumentMapping[arg] = argumentMapping.getValue(arg)
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

fun FirConstKind<*>.expectedConeType(session: FirSession): ConeKotlinType {
    fun constructLiteralType(classId: ClassId, isNullable: Boolean = false): ConeKotlinType {
        val symbol = session.firSymbolProvider.getClassLikeSymbolByFqName(classId)
            ?: return ConeClassErrorType(ConeSimpleDiagnostic("Missing stdlib class: $classId", DiagnosticKind.MissingStdlibClass))
        return symbol.toLookupTag().constructClassType(emptyArray(), isNullable)
    }
    return when (this) {
        FirConstKind.Null -> session.builtinTypes.nullableNothingType.type
        FirConstKind.Boolean -> session.builtinTypes.booleanType.type
        FirConstKind.Char -> constructLiteralType(StandardClassIds.Char)
        FirConstKind.Byte -> constructLiteralType(StandardClassIds.Byte)
        FirConstKind.Short -> constructLiteralType(StandardClassIds.Short)
        FirConstKind.Int -> constructLiteralType(StandardClassIds.Int)
        FirConstKind.Long -> constructLiteralType(StandardClassIds.Long)
        FirConstKind.String -> constructLiteralType(StandardClassIds.String)
        FirConstKind.Float -> constructLiteralType(StandardClassIds.Float)
        FirConstKind.Double -> constructLiteralType(StandardClassIds.Double)

        FirConstKind.UnsignedByte -> constructLiteralType(StandardClassIds.UByte)
        FirConstKind.UnsignedShort -> constructLiteralType(StandardClassIds.UShort)
        FirConstKind.UnsignedInt -> constructLiteralType(StandardClassIds.UInt)
        FirConstKind.UnsignedLong -> constructLiteralType(StandardClassIds.ULong)

        FirConstKind.IntegerLiteral -> constructLiteralType(StandardClassIds.Int)
        FirConstKind.UnsignedIntegerLiteral -> constructLiteralType(StandardClassIds.UInt)
    }
}
