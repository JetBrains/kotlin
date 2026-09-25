/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.analysis.checkers.isJavaValueClass
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
        (this.isPrimitiveOrNullablePrimitive || this.isValueClass(sessionHolder.session) || this.isFlexiblePrimitive())
    ) return true
    return this.isJavaValueBasedClassAndWarningsEnabled()
}

// Like javac, this includes type parameters bounded by a Java value class.
context(sessionHolder: SessionHolder)
internal fun ConeKotlinType.isJavaValueClass(visited: MutableSet<FirTypeParameterSymbol> = mutableSetOf()): Boolean =
    when (this) {
        is ConeFlexibleType -> lowerBound.isJavaValueClass(visited)
        is ConeDefinitelyNotNullType -> original.isJavaValueClass(visited)
        is ConeIntersectionType -> intersectedTypes.any { it.isJavaValueClass(visited) }
        is ConeTypeParameterType ->
            visited.add(lookupTag.symbol) && lookupTag.symbol.resolvedBounds.any { it.coneType.isJavaValueClass(visited) }
        is ConeClassLikeType -> toRegularClassSymbol()?.isJavaValueClass(sessionHolder.session) == true
        is ConeCapturedType, is ConeUnionType, is ConeTypeVariableType, is ConeStubTypeForTypeVariableInSubtyping,
        is ConeIntegerLiteralType,
            -> false
    }

internal fun ConeKotlinType.isFlexiblePrimitive(): Boolean {
    return this is ConeFlexibleType && lowerBound.isPrimitiveOrNullablePrimitive && upperBound.isPrimitiveOrNullablePrimitive
}
