/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp

object TestData {
    // ISSUE: KT-69975, KT-78229
    const val IDENTIFIER_WITH_BACKTICKS_IN_KDOC =
"""/**
 *                        // Resolved?
 * [`top level`]          // Expect: ✅; Actual: ❌
 * [top level]            // Expect: ❌; Actual: ❌
 * [O.with space]         // Expect: ❌; Actual: ❌
 * [O.`with space`]       // Expect: ✅; Actual: ❌
 * @see O.with space      // Expect: ❌; Actual: ❌
 * @see O.`with space`    // Expect: ✅; Actual: ❌
 * [O.]]                  // Expect: ❌; Actual: ❌
 * [O.`]`]                // Expect: ✅; Actual: ❌
 * @see O.]               // Expect: ❌; Actual: ❌
 * @see O.`]`             // Expect: ✅; Actual: ❌
 * [O.without_space]      // Expect: ✅; Actual: ✅
 * [O.`without_space`]    // Expect: ✅; Actual: ❌
 * @see O.without_space   // Expect: ✅; Actual: ✅
 * @see O.`without_space` // Expect: ✅; Actual: ❌
 */
fun main() {
}

fun `top level`() = Unit

object O {
    fun `with space`() = Unit
    fun `]`() = Unit // Disallowed in JVM: invalid character ']'
    fun without_space() = Unit
}"""
}