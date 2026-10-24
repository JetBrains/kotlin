/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.analysis.checkers.JDK_INTERNAL_VALUE_BASED_ANNOTATION_CLASS_ID
import org.jetbrains.kotlin.fir.analysis.checkers.anyBound
import org.jetbrains.kotlin.fir.analysis.checkers.isBoundedByPrimitiveOrValueClass
import org.jetbrains.kotlin.fir.analysis.checkers.isFlexiblePrimitive
import org.jetbrains.kotlin.fir.analysis.checkers.isValueClass
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration.isCompiledAsIdentityClass
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.isJvmTargetValhallaCompatible
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isInlineOrValue
import org.jetbrains.kotlin.fir.enableWarningsForIdentitySensitiveOperationsOnValueClassesAndPrimitives
import org.jetbrains.kotlin.fir.enableWarningsForValueBasedJavaClasses
import org.jetbrains.kotlin.fir.isPlatformValueClass
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
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

// Whether instances of this type are value objects at run time (JEP 401): with a Valhalla-compatible JVM target, boxed primitives and
// Java value classes are, and so are Kotlin value classes compiled as Valhalla value classes.
context(sessionHolder: SessionHolder)
internal fun ConeKotlinType.isValueObjectAtRuntime(): Boolean {
    val session = sessionHolder.session
    if (!session.isJvmTargetValhallaCompatible) return false
    if (isFlexiblePrimitive()) return true
    return anyBound {
        val symbol = it.toRegularClassSymbol(session)
        it.isPrimitiveOrNullablePrimitive || symbol?.isPlatformValueClass(session) == true ||
                symbol?.isInlineOrValue == true && !symbol.isCompiledAsIdentityClass(session)
    }
}

// A flexible primitive type like `Int!` is its Java box, like `java.lang.Integer`, so it is a value class only if its platform class is.
context(sessionHolder: SessionHolder)
internal fun ConeKotlinType.isValueClassOrPrimitive(): Boolean =
    if (isFlexiblePrimitive()) isValueClass(sessionHolder.session)
    else isBoundedByPrimitiveOrValueClass(sessionHolder.session)

