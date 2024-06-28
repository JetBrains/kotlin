/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.overrides

interface IrExternalOverridabilityCondition {
    enum class Result {
        OVERRIDABLE,
        INCOMPATIBLE,
        UNKNOWN
    }

    enum class Contract {
        CONFLICTS_ONLY,
        SUCCESS_ONLY,
        BOTH
    }

    /**
     * Determines whether [superMember] is overridable by [subMember]. The returned result can be one of the following:
     *
     *   - [Result.OVERRIDABLE] means that it is definitely overridable, and neither the general override checking algorithm, nor other
     *     external overridability conditions can refute that.
     *   - [Result.INCOMPATIBLE] means that it is not overridable, but it's OK to have both declarations available in the same class because
     *     they don't cause a conflict.
     *   - [Result.UNKNOWN] means that this overridability condition cannot claim anything about the overridability, and the final result
     *     will be based on the general override checking algorithm and the other external overridability conditions.
     */
    fun isOverridable(
        superMember: MemberWithOriginal,
        subMember: MemberWithOriginal,
    ): Result

    /**
     * Specifies what values can be returned by [isOverridable]. Used as an optimization to reorder overridability conditions to prevent
     * redundant calculations.
     *
     *   - [Contract.CONFLICTS_ONLY] means that [isOverridable] can return [Result.INCOMPATIBLE] or [Result.UNKNOWN].
     *   - [Contract.SUCCESS_ONLY] means that [isOverridable] can return [Result.OVERRIDABLE] or [Result.UNKNOWN].
     *   - [Contract.BOTH] means that [isOverridable] can return any result.
     */
    val contract: Contract
}
