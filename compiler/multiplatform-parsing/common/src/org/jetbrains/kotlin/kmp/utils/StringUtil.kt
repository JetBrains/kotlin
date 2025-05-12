/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.utils

class StringUtil {
    companion object {
        fun getLineBreakCount(text: CharSequence): Int {
            var count = 0
            var i = 0
            while (i < text.length) {
                val c = text[i]
                when (c) {
                    '\n' -> {
                        count++
                    }
                    '\r' -> {
                        if ((i + 1).let { it < text.length && text[it] == '\n' }) {
                            i++
                        }
                        count++
                    }
                }
                i++
            }
            return count
        }

        fun containsLineBreak(text: CharSequence): Boolean {
            for (i in 0..<text.length) {
                if (isLineBreak(text[i])) return true
            }
            return false
        }

        fun isLineBreak(c: Char): Boolean = c == '\n' || c == '\r'
    }
}