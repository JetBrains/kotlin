/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// JVM_ABI_K1_K2_DIFF: KT-63864
// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

fun foo(x: Number) {
    sb.appendLine(x.toByte())
}

fun box(): String {
    foo(18)
    val nonConst = 18
    foo(nonConst)

    assertEquals("18\n18\n", sb.toString())
    return "OK"
}