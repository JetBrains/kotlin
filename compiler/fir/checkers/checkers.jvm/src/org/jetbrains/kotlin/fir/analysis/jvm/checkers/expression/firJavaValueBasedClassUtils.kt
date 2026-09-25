/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.analysis.checkers.isJavaValueClass
import org.jetbrains.kotlin.fir.analysis.checkers.isMappedToJavaValueClass
import org.jetbrains.kotlin.fir.analysis.checkers.isValueClass
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.enableWarningsForIdentitySensitiveOperationsOnValueClassesAndPrimitives
import org.jetbrains.kotlin.fir.enableWarningsForValueBasedJavaClasses
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.symbol
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
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.name.ClassId

private val jdkInternalValueBasedAnnotationClassId = ClassId.fromString("jdk/internal/ValueBased")

context(sessionHolder: SessionHolder)
internal fun ConeKotlinType.isJavaValueBasedClass(): Boolean {
    val classSymbol = toClassSymbol() ?: return false
    return classSymbol.hasAnnotation(jdkInternalValueBasedAnnotationClassId, sessionHolder.session)
}

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

// Like javac, this includes type parameters and captured types bounded by a primitive or a value class. A flexible primitive type like
// `Int!` is its Java box, like `java.lang.Integer`.
context(sessionHolder: SessionHolder)
internal fun ConeKotlinType.isValueClassOrPrimitive(): Boolean =
    if (isFlexiblePrimitive()) isJavaValueClass()
    else anyBound { it.isPrimitiveOrNullablePrimitive || it.isValueClass(sessionHolder.session) }

// Like javac, this includes type parameters and captured types bounded by a Java value class. A flexible primitive type like `Int!` is
// its Java box, like `java.lang.Integer`.
context(sessionHolder: SessionHolder)
internal fun ConeKotlinType.isJavaValueClass(): Boolean =
    if (isFlexiblePrimitive()) lowerBoundIfFlexible().toRegularClassSymbol()?.isMappedToJavaValueClass(sessionHolder.session) == true
    else anyBound { it.toRegularClassSymbol()?.isJavaValueClass(sessionHolder.session) == true }

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

internal fun ConeKotlinType.isFlexiblePrimitive(): Boolean {
    return this is ConeFlexibleType && lowerBound.isPrimitiveOrNullablePrimitive && upperBound.isPrimitiveOrNullablePrimitive
}
