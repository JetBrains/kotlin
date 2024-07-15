/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.substitution

import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeProjection

//@ConsistentCopyVisibility
@Suppress("DATA_CLASS_COPY_VISIBILITY_WILL_BE_CHANGED_WARNING")
data class ChainedSubstitutor private constructor(val first: ConeSubstitutor, val second: ConeSubstitutor) : ConeSubstitutor() {
    companion object {
        operator fun invoke(first: ConeSubstitutor, second: ConeSubstitutor): ConeSubstitutor {
            if (first == Empty) return second
            if (second == Empty) return first
            return ChainedSubstitutor(first, second)
        }
    }

    override fun substituteOrNull(type: ConeKotlinType): ConeKotlinType? {
        first.substituteOrNull(type)?.let { return second.substituteOrSelf(it) }
        return second.substituteOrNull(type)
    }

    override fun substituteArgument(projection: ConeTypeProjection, index: Int): ConeTypeProjection? {
        val firstResult = first.substituteArgument(projection, index)
        return second.substituteArgument(firstResult ?: projection, index) ?: firstResult
    }

    override fun toString(): String {
        return "$first then $second"
    }
}

fun ConeSubstitutor.chain(other: ConeSubstitutor): ConeSubstitutor {
    return ChainedSubstitutor(this, other)
}
