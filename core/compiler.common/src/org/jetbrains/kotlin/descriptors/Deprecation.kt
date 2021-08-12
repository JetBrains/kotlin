/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors

data class Deprecation(
    val level: DeprecationLevelValue,
    val inheritable: Boolean,
    val message: String? = null
) : Comparable<Deprecation> {
    override fun compareTo(other: Deprecation): Int {
        val lr = level.compareTo(other.level)
        //to prefer inheritable deprecation
        return if (lr == 0 && !inheritable && other.inheritable) 1
        else lr
    }
}

/**
 * This corresponds to [DeprecationLevel] in Kotlin standard library. A symbol annotated with [java.lang.Deprecated] is considered a
 * warning.
 */
enum class DeprecationLevelValue {
    WARNING, ERROR, HIDDEN
}
