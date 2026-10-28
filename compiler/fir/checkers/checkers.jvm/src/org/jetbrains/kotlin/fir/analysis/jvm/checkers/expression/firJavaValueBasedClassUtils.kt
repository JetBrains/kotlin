/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.analysis.checkers.IdentityKind
import org.jetbrains.kotlin.fir.analysis.checkers.identityKind
import org.jetbrains.kotlin.fir.analysis.checkers.isFlexiblePrimitive
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration.isCompiledAsIdentityClass
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.isJvmTargetValhallaCompatible
import org.jetbrains.kotlin.fir.collectUpperBounds
import org.jetbrains.kotlin.fir.declarations.utils.isValue
import org.jetbrains.kotlin.fir.enableWarningsForIdentitySensitiveOperationsOnValueClassesAndPrimitives
import org.jetbrains.kotlin.fir.enableWarningsForValueBasedJavaClasses
import org.jetbrains.kotlin.fir.isPlatformValueClass
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.isPrimitiveOrNullablePrimitive
import org.jetbrains.kotlin.fir.types.typeContext

context(sessionHolder: SessionHolder)
internal fun ConeKotlinType.isValueTypeAndWarningsEnabled(): Boolean = when (identityKind()) {
    IdentityKind.VALUE_TYPE, IdentityKind.FLEXIBLE_PRIMITIVE -> enableWarningsForIdentitySensitiveOperationsOnValueClassesAndPrimitives()
    IdentityKind.VALUE_BASED -> enableWarningsForValueBasedJavaClasses()
    null -> false
}

// Whether instances of this type are value objects at run time (JEP 401): with a Valhalla-compatible JVM target, boxed primitives and
// Java value classes are, and so are Kotlin value classes compiled as Valhalla value classes.
context(sessionHolder: SessionHolder)
internal fun ConeKotlinType.isValueObjectAtRuntime(): Boolean {
    val session = sessionHolder.session
    if (!session.isJvmTargetValhallaCompatible) return false
    if (isFlexiblePrimitive()) return true
    return collectUpperBounds(session.typeContext).any {
        val symbol = it.toRegularClassSymbol(session)
        it.isPrimitiveOrNullablePrimitive || symbol?.isPlatformValueClass(session) == true ||
                symbol?.isValue == true && !symbol.isCompiledAsIdentityClass(session)
    }
}
