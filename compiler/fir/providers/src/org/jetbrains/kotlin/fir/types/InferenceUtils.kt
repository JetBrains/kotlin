/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.AbstractTypeChecker
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
private fun ConeKotlinType.classId(session: FirSession): ClassId? {
    contract {
        returns(true) implies (this@classId is ConeClassLikeType)
    }
    if (this !is ConeClassLikeType) return null
    return fullyExpandedType(session).lookupTag.classId
}

fun ConeKotlinType.isKProperty(session: FirSession): Boolean {
    val classId = classId(session) ?: return false
    return classId.packageFqName == StandardClassIds.BASE_REFLECT_PACKAGE &&
            classId.shortClassName.identifier.startsWith("KProperty")
}

fun ConeKotlinType.isKMutableProperty(session: FirSession): Boolean {
    val classId = classId(session) ?: return false
    return classId.packageFqName == StandardClassIds.BASE_REFLECT_PACKAGE &&
            classId.shortClassName.identifier.startsWith("KMutableProperty")
}

fun ConeKotlinType.isKClassType(): Boolean {
    return classId == StandardClassIds.KClass
}

val FirAnonymousFunction.returnType: ConeKotlinType? get() = returnTypeRef.coneTypeSafe()
val FirAnonymousFunction.receiverType: ConeKotlinType? get() = receiverParameter?.typeRef?.coneTypeSafe()

fun ConeTypeContext.isTypeMismatchDueToNullability(
    actualType: ConeKotlinType,
    expectedType: ConeKotlinType
): Boolean {
    return actualType.isNullableType() && !expectedType.isNullableType() && AbstractTypeChecker.isSubtypeOf(
        this,
        actualType,
        expectedType.withNullability(ConeNullability.NULLABLE, this)
    )
}
