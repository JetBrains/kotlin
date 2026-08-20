/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.findArgumentByName
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.getTargetType
import org.jetbrains.kotlin.fir.expressions.FirCollectionLiteral
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.ConeIntegerLiteralType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.isMarkedNullable
import org.jetbrains.kotlin.fir.types.isPrimitiveNumberOrNullableType
import org.jetbrains.kotlin.fir.types.isUnsignedTypeOrNullableUnsignedType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.addToStdlib.ensureIsInstance

fun FirBasedSymbol<*>.supportsNumericClassConversionFrom(type: ConeKotlinType, session: FirSession): Boolean =
    getSupportedNumericClassConversions(session)?.any { type.fitsInto(it) } ?: false

fun FirBasedSymbol<*>.supportsNumericClassConversionTo(type: ConeKotlinType, session: FirSession): Boolean =
    getSupportedNumericClassConversions(session)?.all { it.fitsInto(type) } ?: false

fun FirBasedSymbol<*>.getSupportedNumericClassConversions(session: FirSession): List<ConeKotlinType>? {
    lazyResolveToPhase(FirResolvePhase.ANNOTATION_ARGUMENTS)

    val actualizationsArgument = getAnnotationByClassId(StandardClassIds.Annotations.NumericClass, session)
        ?.findArgumentByName(Name.identifier("actualizations"), returnFirstWhenNotFound = false)
        ?: return null

    val arguments = when (actualizationsArgument) {
        is FirVarargArgumentsExpression -> actualizationsArgument.arguments
        is FirCollectionLiteral -> actualizationsArgument.arguments
        else -> return null
    }

    return arguments.ensureIsInstance<FirGetClassCall>()?.mapNotNull { it.getTargetType() }
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
