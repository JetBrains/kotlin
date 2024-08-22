/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.KtSourceElement

class StringLiteralsFolder {
    private val sb = StringBuilder()
    private var source: KtSourceElement? = null
    private var canFold: Boolean = true
    private var empty: Boolean = true

    fun checkpoint(): StringLiteralsFolderCheckpoint {
        assert(source != null)
        return StringLiteralsFolderCheckpoint(sb.length)
    }

    fun fold(item: String) {
        assert(source != null)
        assert(canFold)
        empty = false
        sb.append(item)
    }

    fun release(): Pair<String, KtSourceElement> {
        assert(source != null)
        assert(!empty)
        canFold = true
        empty = true
        val result = sb.toString()
        sb.clear()
        return Pair(result, source!!)
    }

    fun canFold(): Boolean {
        return canFold
    }

    fun canInitialize(): Boolean {
        return empty
    }

    fun disable() {
        canFold = false
    }

    fun initialize(source: KtSourceElement) {
        assert(empty)
        canFold = true
        this.source = source
    }

    data class StringLiteralsFolderCheckpoint(private val index: Int) {
        fun isStart(): Boolean {
            return index == 0
        }

        fun rollback(folder: StringLiteralsFolder) {
            assert(folder.source != null)
            folder.sb.delete(index, folder.sb.length)
            if (folder.sb.isEmpty()) {
                folder.empty = true
            }
        }
    }
}

