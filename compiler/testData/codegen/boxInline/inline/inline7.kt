/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// WITH_STDLIB
// FILE: lib.kt

val sb = StringBuilder()

@Suppress("NOTHING_TO_INLINE")
inline fun foo(vararg args: Int) {
    for (a in args) {
        sb.appendLine(a.toString())
    }
}

// FILE: main.kt
import kotlin.test.*

fun bar() {
    foo(1, 2, 3)
}

fun box(): String {
    bar()

    assertEquals("""
        1
        2
        3

    """.trimIndent(), sb.toString())
    return "OK"
}
