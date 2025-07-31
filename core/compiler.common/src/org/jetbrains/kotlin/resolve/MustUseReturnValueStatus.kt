/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

enum class ReturnValueStatus {
    MustUse,
    ExplicitlyIgnorable,
    Unspecified,
    ;

    companion object {
        fun fromBitFlags(hasMustUseReturnValue: Boolean, hasIgnorableReturnValue: Boolean): ReturnValueStatus {
            if (hasMustUseReturnValue && hasIgnorableReturnValue) error("State is incorrect: cannot be both must use and explicitly ignorable")
            if (hasMustUseReturnValue) return MustUse
            if (hasIgnorableReturnValue) return ExplicitlyIgnorable
            return Unspecified
        }
    }
}
