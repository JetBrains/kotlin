/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// JVM_ABI_K1_K2_DIFF: KT-63864
// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

val lazyValue: String by lazy {
    sb.appendLine("computed!")
    "Hello"
}

fun box(): String {
    sb.appendLine(lazyValue)
    sb.appendLine(lazyValue)

    assertEquals("""
        computed!
        Hello
        Hello

    """.trimIndent(), sb.toString())
    return "OK"
}