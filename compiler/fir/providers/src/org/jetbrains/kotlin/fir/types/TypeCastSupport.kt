/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol

/**
 * Exists because the CS system is otherwise not accessible to checkers.
 */
interface TypeCastSupport : FirSessionComponent {
    /**
     * Returns `true` if the cast to [targetType] can be safely checked at runtime given
     * that the object casted is already of time [type].
     *
     * Example 1:
     * - [type] = `Collection<String>`
     * - [targetType] = `List<CharSequence>`
     * - result = `true`, checks for `classId`s are reliable, and we know all possible
     *                    `List`s of `Collection<String>` must be `List<CharSequence>`
     *
     * Example 2:
     * - [type] = `Any`
     * - [targetType] = `List<CharSequence>`
     * - result = `false`, it could be `List<Any>`, we don't know that
     *
     * Example 3:
     * - [type] = `List<String>`
     * - [targetType] = `MutableList<out String>`
     * - result = `true`, we know how arguments of `MutableList` relate to `List`
     */
    fun isCastToTargetTypeErased(
        targetType: ConeKotlinType,
        type: ConeKotlinType,
        session: FirSession,
    ): Boolean

    /**
     * Returns the approximation of the safest downcast from [supertype] to [subTypeClassSymbol]
     * (the actual safest downcast may require more complex relations between arguments than those
     * representable in the form of a single `ConeKotlinType`).
     *
     * Example 1:
     * - [supertype] = `Collection<String>`
     * - [subTypeClassSymbol] = `List`
     * - result = `List<String>`, all arguments are inferred
     *
     * Example 2:
     * - [supertype] = `Any`
     * - [subTypeClassSymbol] = `List`
     * - result = `List<*>`, some arguments were not inferred, replaced with '*'
     *
     * Example 3:
     * - [supertype] = `List<String>`
     * - [subTypeClassSymbol] = `MutableList`
     * - result = `MutableList<out String>`, all arguments are inferred
     */
    fun findStaticallyKnownSubtype(
        supertype: ConeKotlinType,
        subTypeClassSymbol: FirRegularClassSymbol,
        isSubTypeMarkedNullable: Boolean,
        attributes: ConeAttributes,
        session: FirSession,
    ): ConeKotlinType?
}

val FirSession.typeCastSupport: TypeCastSupport by FirSession.sessionComponentAccessor()
