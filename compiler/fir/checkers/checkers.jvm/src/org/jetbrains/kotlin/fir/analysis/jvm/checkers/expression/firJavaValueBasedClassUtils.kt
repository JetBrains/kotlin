/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.analysis.checkers.isValueClass
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.enableWarningsForIdentitySensitiveOperationsOnValueClassesAndPrimitives
import org.jetbrains.kotlin.fir.enableWarningsForValueBasedJavaClasses
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
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

internal fun ConeKotlinType.isFlexiblePrimitive(): Boolean {
    return this is ConeFlexibleType && lowerBound.isPrimitiveOrNullablePrimitive && upperBound.isPrimitiveOrNullablePrimitive
}
