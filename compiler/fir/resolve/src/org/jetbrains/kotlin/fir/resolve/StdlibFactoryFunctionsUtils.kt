/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.arrayElementType
import org.jetbrains.kotlin.fir.types.asCone
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.isList
import org.jetbrains.kotlin.fir.types.isMutableList
import org.jetbrains.kotlin.fir.types.isMutableSet
import org.jetbrains.kotlin.fir.types.isNonPrimitiveArray
import org.jetbrains.kotlin.fir.types.isPrimitiveArray
import org.jetbrains.kotlin.fir.types.isSequence
import org.jetbrains.kotlin.fir.types.isSet
import org.jetbrains.kotlin.fir.types.isUnsignedArray
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.ArrayFqNames
import org.jetbrains.kotlin.types.model.withNullability
import kotlin.collections.get

fun toArrayOfFactoryName(
    expectedType: ConeKotlinType,
    session: FirSession,
    // if the expectedType is non-array, controls whether to return `arrayOf` or not
    eagerlyReturnNonPrimitive: Boolean,
): Name? {
    val coneType = expectedType.fullyExpandedType(session)
    return when {
        coneType.isPrimitiveArray -> {
            val arrayElementClassId = coneType.arrayElementType()!!.classId
            val primitiveType = PrimitiveType.getByShortName(arrayElementClassId!!.shortClassName.asString())
            ArrayFqNames.PRIMITIVE_TYPE_TO_ARRAY[primitiveType]!!
        }
        coneType.isUnsignedArray -> {
            val arrayElementClassId = coneType.arrayElementType()!!.classId
            ArrayFqNames.UNSIGNED_TYPE_TO_ARRAY[arrayElementClassId!!.asSingleFqName()]!!
        }
        coneType.isNonPrimitiveArray -> {
            ArrayFqNames.ARRAY_OF_FUNCTION
        }
        else -> {
            if (!eagerlyReturnNonPrimitive) return null
            ArrayFqNames.ARRAY_OF_FUNCTION
        }
    }
}

fun toCollectionOfFactoryPackageAndName(
    expectedType: ConeKotlinType,
    session: FirSession,
): Pair<FqName, Name>? {

    toArrayOfFactoryName(expectedType, session, eagerlyReturnNonPrimitive = false)?.let {
        return StandardNames.BUILT_INS_PACKAGE_FQ_NAME to it
    }

    val coneType = with(session.typeContext) { expectedType.fullyExpandedType(session).withNullability(false).asCone() }

    return when {
        coneType.isList -> StandardNames.COLLECTIONS_PACKAGE_FQ_NAME to Name.identifier("listOf")
        coneType.isMutableList -> StandardNames.COLLECTIONS_PACKAGE_FQ_NAME to Name.identifier("mutableListOf")
        coneType.isSet -> StandardNames.COLLECTIONS_PACKAGE_FQ_NAME to Name.identifier("setOf")
        coneType.isMutableSet -> StandardNames.COLLECTIONS_PACKAGE_FQ_NAME to Name.identifier("mutableSetOf")
        coneType.isSequence -> StandardNames.SEQUENCES_PACKAGE_FQ_NAME to Name.identifier("sequenceOf")
        else -> null
    }
}
