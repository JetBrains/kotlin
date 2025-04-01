/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

object TypeVariableDirectionCalculator {
    enum class ResolveDirection {
        TO_SUBTYPE,
        TO_SUPERTYPE,
        UNKNOWN
    }
}
