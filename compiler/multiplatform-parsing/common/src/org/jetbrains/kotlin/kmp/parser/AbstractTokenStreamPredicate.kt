/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.parser

abstract class AbstractTokenStreamPredicate {
    abstract fun matching(topLevel: Boolean): Boolean

    fun or(other: AbstractTokenStreamPredicate): AbstractTokenStreamPredicate {
        return object : AbstractTokenStreamPredicate() {
            override fun matching(topLevel: Boolean): Boolean {
                if (this@AbstractTokenStreamPredicate.matching(topLevel)) return true
                return other.matching(topLevel)
            }
        }
    }
}