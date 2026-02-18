/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.ArrayFqNames
import org.jetbrains.kotlin.resolve.CollectionNames
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
    expectedClass: FirRegularClassSymbol,
    session: FirSession,
): Pair<FqName, Name>? {
    toArrayOfFactoryName(expectedClass.defaultType(), session, eagerlyReturnNonPrimitive = false)?.let {
        return StandardNames.BUILT_INS_PACKAGE_FQ_NAME to it
    }

    return when (expectedClass.classId) {
        StandardClassIds.List -> StandardNames.COLLECTIONS_PACKAGE_FQ_NAME to CollectionNames.Factories.LIST_OF
        StandardClassIds.MutableList -> StandardNames.COLLECTIONS_PACKAGE_FQ_NAME to CollectionNames.Factories.MUTABLE_LIST_OF
        StandardClassIds.Set -> StandardNames.COLLECTIONS_PACKAGE_FQ_NAME to CollectionNames.Factories.SET_OF
        StandardClassIds.MutableSet -> StandardNames.COLLECTIONS_PACKAGE_FQ_NAME to CollectionNames.Factories.MUTABLE_SET_OF
        StandardClassIds.Sequence -> StandardNames.SEQUENCES_PACKAGE_FQ_NAME to CollectionNames.Factories.SEQUENCE_OF
        else -> null
    }
}
