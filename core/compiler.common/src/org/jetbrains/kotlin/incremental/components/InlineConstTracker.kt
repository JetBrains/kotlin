/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.components

interface InlineConstTracker {
    fun report(filePath: String, owner: String, name: String, constType: String)

    object DoNothing : InlineConstTracker {
        override fun report(filePath: String, owner: String, name: String, constType: String) {
        }
    }
}
