/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// JVM_ABI_K1_K2_DIFF: KT-63864
// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    var previous: Any? = null
    for (i in 0 .. 2) {
        class Outer {
            inner class Inner {
                override fun toString() = i.toString()
            }

            override fun toString() = Inner().toString()
        }
        if (previous != null) sb.appendLine(previous.toString())
        previous = Outer()
    }

    assertEquals("""
        0
        1

    """.trimIndent(), sb.toString())
    return "OK"
}
