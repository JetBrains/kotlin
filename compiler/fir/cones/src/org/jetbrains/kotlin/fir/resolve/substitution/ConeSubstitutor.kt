/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.substitution

import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.types.model.TypeSubstitutorMarker

abstract class ConeSubstitutor : TypeSubstitutorMarker {
    open fun substituteOrSelf(type: ConeKotlinType): ConeKotlinType = substituteOrNull(type) ?: type
    abstract fun substituteOrNull(type: ConeKotlinType): ConeKotlinType?

    object Empty : ConeSubstitutor() {
        override fun substituteOrSelf(type: ConeKotlinType): ConeKotlinType {
            return type
        }

        override fun substituteOrNull(type: ConeKotlinType): ConeKotlinType? {
            return null
        }

        override fun toString(): String = "Empty"
    }
}

fun ConeSubstitutor.substituteOrNull(type: ConeKotlinType?): ConeKotlinType? {
    return type?.let { substituteOrNull(it) }
}

object NoSubstitutor : TypeSubstitutorMarker
