/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent

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
}

val FirSession.typeCastSupport: TypeCastSupport by FirSession.sessionComponentAccessor()
