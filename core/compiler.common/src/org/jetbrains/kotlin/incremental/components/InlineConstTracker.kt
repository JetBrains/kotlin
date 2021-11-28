/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.components

/**
 * InlineConstTracker is used to track Java constants used in Kotlin for correct build scope expansion in IC during JPS build.
 */
interface InlineConstTracker {

    /**
     * Report Java constant, which is defined as [name] in [owner] java class.
     * This constant is used in Kotlin file [filePath].
     * [constType] is one of Kotlin's [Byte, Short, Int, Long, Float, Double, Boolean, Char, String],
     * that correspond to the eight primitive Java types or String
     * Format of [owner] class is "package.Outer$Inner"
     */
    fun report(filePath: String, owner: String, name: String, constType: String)

    object DoNothing : InlineConstTracker {
        override fun report(filePath: String, owner: String, name: String, constType: String) {
        }
    }
}
