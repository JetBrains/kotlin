/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// JVM_ABI_K1_K2_DIFF: KT-63864
// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    val str = "captured"
    foo {
        sb.appendLine(it)
        sb.appendLine(str)
    }
    assertEquals("""
        42
        captured

    """.trimIndent(), sb.toString())
    return "OK"

}

fun foo(f: (Int) -> Unit) {
    f(42)
}