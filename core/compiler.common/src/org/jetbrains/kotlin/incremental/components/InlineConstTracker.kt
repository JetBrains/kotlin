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
     * Report Java constant, which is defined as [name] in [owner] java class and has primitive type,
     * which is converted to kotlin's type [constType].
     * This constant is used in Kotlin file [filePath].
     */
    fun report(filePath: String, owner: String, name: String, constType: String)

    object DoNothing : InlineConstTracker {
        override fun report(filePath: String, owner: String, name: String, constType: String) {
        }
    }
}
