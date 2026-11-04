/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.analysis.checkers.isFlexiblePrimitive
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration.isCompiledAsIdentityClass
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.isJvmTargetValhallaCompatible
import org.jetbrains.kotlin.fir.collectUpperBounds
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isValue
import org.jetbrains.kotlin.fir.isPlatformValueClass
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.isPrimitiveOrNullablePrimitive
import org.jetbrains.kotlin.fir.types.typeContext

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
                symbol?.isValue == true && !symbol.isCompiledAsIdentityClass(session) && !symbol.isOrExtendsExpectClass(session)
    }
}

// An expect class is only seen in metadata compilation, as platform checkers see its actual class otherwise. That may be an identity
// class or compiled as one, and so may its subclasses.
private fun FirRegularClassSymbol.isOrExtendsExpectClass(session: FirSession): Boolean =
    isExpect || resolvedSuperTypes.any { it.toRegularClassSymbol(session)?.isOrExtendsExpectClass(session) == true }
