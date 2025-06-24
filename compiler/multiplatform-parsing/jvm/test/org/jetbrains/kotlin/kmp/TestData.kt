/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp

object TestData {
    // ISSUE: KT-69975, KT-78229
    const val IDENTIFIER_WITH_BACKTICKS_IN_KDOC =
"""/**
 *
 * [`top level`]
 * [top level]
 * [O.with space]
 * [O.`with space`]
 * @see O.with space
 * @see O.`with space`
 * [O.]]
 * [O.`]`]
 * @see O.]
 * @see O.`]`
 * [O.without_space]
 * [O.`without_space`]
 * @see O.without_space
 * @see O.`without_space`
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