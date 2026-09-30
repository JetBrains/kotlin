/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.analysis.checkers.JDK_INTERNAL_VALUE_BASED_ANNOTATION_CLASS_ID
import org.jetbrains.kotlin.fir.analysis.checkers.isFlexiblePrimitive
import org.jetbrains.kotlin.fir.analysis.checkers.isValueClass
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.enableWarningsForIdentitySensitiveOperationsOnValueClassesAndPrimitives
import org.jetbrains.kotlin.fir.enableWarningsForValueBasedJavaClasses
import org.jetbrains.kotlin.fir.resolve.symbol
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeCapturedType
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeDefinitelyNotNullType
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.ConeIntegerLiteralType
import org.jetbrains.kotlin.fir.types.ConeIntersectionType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeStubTypeForTypeVariableInSubtyping
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.ConeTypeVariableType
import org.jetbrains.kotlin.fir.types.ConeUnionType
import org.jetbrains.kotlin.fir.types.isPrimitiveOrNullablePrimitive

context(sessionHolder: SessionHolder)
internal fun ConeKotlinType.isJavaValueBasedClass(): Boolean =
    anyBound { it.toClassSymbol()?.hasAnnotation(JDK_INTERNAL_VALUE_BASED_ANNOTATION_CLASS_ID, sessionHolder.session) == true }

context(sessionHolder: SessionHolder)
internal fun ConeKotlinType.isJavaValueBasedClassAndWarningsEnabled(): Boolean {
    return enableWarningsForValueBasedJavaClasses() && this.isJavaValueBasedClass()
}

context(sessionHolder: SessionHolder)
internal fun ConeKotlinType.isValueTypeAndWarningsEnabled(): Boolean {
    if (enableWarningsForIdentitySensitiveOperationsOnValueClassesAndPrimitives() &&
        (this.isFlexiblePrimitive() || this.isValueClassOrPrimitive())
    ) return true
    return this.isJavaValueBasedClassAndWarningsEnabled()
}

// A flexible primitive type like `Int!` is its Java box, like `java.lang.Integer`, so it is a value class only if its platform class is.
context(sessionHolder: SessionHolder)
internal fun ConeKotlinType.isValueClassOrPrimitive(): Boolean =
    if (isFlexiblePrimitive()) isValueClass(sessionHolder.session)
    else anyBound { it.isPrimitiveOrNullablePrimitive || it.isValueClass(sessionHolder.session) }

private fun ConeKotlinType.anyBound(
    visited: MutableSet<FirTypeParameterSymbol> = mutableSetOf(),
    predicate: (ConeClassLikeType) -> Boolean,
): Boolean =
    when (this) {
        is ConeFlexibleType -> lowerBound.anyBound(visited, predicate)
        is ConeDefinitelyNotNullType -> original.anyBound(visited, predicate)
        is ConeIntersectionType -> intersectedTypes.any { it.anyBound(visited, predicate) }
        is ConeTypeParameterType ->
            visited.add(lookupTag.symbol) && lookupTag.symbol.resolvedBounds.any { it.coneType.anyBound(visited, predicate) }
        is ConeCapturedType -> constructor.supertypes.orEmpty().any { it.anyBound(visited, predicate) }
        is ConeClassLikeType -> predicate(this)
        is ConeUnionType, is ConeTypeVariableType, is ConeStubTypeForTypeVariableInSubtyping, is ConeIntegerLiteralType,
            -> false
    }
