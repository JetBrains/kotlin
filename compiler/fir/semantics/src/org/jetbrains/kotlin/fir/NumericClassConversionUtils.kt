/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.findArgumentByName
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.getTargetType
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.expressions.builder.buildNumericClassConversion
import org.jetbrains.kotlin.fir.expressions.unwrapAndFlattenArgument
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.ConeIntegerLiteralType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.isMarkedNullable
import org.jetbrains.kotlin.fir.types.isPrimitiveNumberOrNullableType
import org.jetbrains.kotlin.fir.types.isUnsignedTypeOrNullableUnsignedType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.addToStdlib.eachIsInstanceOrNull

fun FirExpression.wrapIntoNumericClassConversionIfNeeded(expectedType: ConeKotlinType, session: FirSession): FirExpression = when {
    !isNumericConversionPossibleBetween(resolvedType, expectedType, session) -> this
    else -> wrapIntoNumericClassConversionTo(expectedType, session)
}

fun FirExpression.wrapIntoNumericClassConversionTo(expectedType: ConeKotlinType, session: FirSession): FirExpression =
    buildNumericClassConversion {
        coneTypeOrNull = expectedType
        originalExpression = this@wrapIntoNumericClassConversionTo
        source = this@wrapIntoNumericClassConversionTo.source?.fakeElement(KtFakeSourceElementKind.NumericClassConversion)
    }

fun isNumericConversionPossibleBetween(from: ConeKotlinType, to: ConeKotlinType, session: FirSession): Boolean {
    return to.toSymbol(session)?.supportsNumericClassConversionFrom(from, session) == true ||
            from.toSymbol(session)?.supportsNumericClassConversionTo(to, session) == true
}

private fun FirBasedSymbol<*>.supportsNumericClassConversionFrom(type: ConeKotlinType, session: FirSession): Boolean =
    getSupportedNumericClassConversions(session)?.any { type.fitsInto(it) } ?: false

private fun FirBasedSymbol<*>.supportsNumericClassConversionTo(type: ConeKotlinType, session: FirSession): Boolean =
    getSupportedNumericClassConversions(session)?.all { it.fitsInto(type) } ?: false

fun FirBasedSymbol<*>.getSupportedNumericClassConversions(session: FirSession): List<ConeKotlinType>? {
    lazyResolveToPhase(FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS)

    val arguments = (getAnnotationByClassId(StandardClassIds.Annotations.NumericClass, session) as? FirAnnotationCall)
        ?.arguments?.flatMap { it.unwrapAndFlattenArgument(flattenArrays = true) }
        ?: return null

    return arguments.eachIsInstanceOrNull<FirPropertyAccessExpression>()
        ?.mapNotNull { enumExpression ->
            StandardClassIds.allIntegerTypes.firstOrNull { it.shortClassName == enumExpression.calleeReference.name }
                ?.defaultType(emptyList())
        }
}

private fun ConeKotlinType.fitsInto(other: ConeKotlinType): Boolean {
    val primitiveClassIds = when {
        isMarkedNullable && !other.isMarkedNullable -> return false
        this is ConeIntegerLiteralType && possibleTypes.any { it == other } -> return true
        isPrimitiveNumberOrNullableType && other.isPrimitiveNumberOrNullableType -> StandardClassIds.signedIntegerTypes
        isUnsignedTypeOrNullableUnsignedType && other.isUnsignedTypeOrNullableUnsignedType -> StandardClassIds.unsignedTypes
        else -> return false
    }
    return primitiveClassIds.indexOf(classId) <= primitiveClassIds.indexOf(other.classId)
}
