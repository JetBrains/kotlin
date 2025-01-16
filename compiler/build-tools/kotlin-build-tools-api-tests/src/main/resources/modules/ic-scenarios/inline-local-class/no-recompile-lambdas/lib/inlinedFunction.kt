/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

private val unusedBefore1 = { 1 }
private val unusedBefore2 = { "unused" }

inline fun calculate(): Int {
    val lambda = {
        40 + 2
    }
    return lambda()
}

private val unusedAfter1 = { true }
private val unusedAfter2 = { 42.0 }
