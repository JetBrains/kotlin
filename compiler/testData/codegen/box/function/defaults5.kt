/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// JVM_ABI_K1_K2_DIFF: KT-63864
// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

class TestClass(val x: Int) {
    fun foo(y: Int = x) {
        sb.appendLine(y)
    }
}

fun TestClass.bar(y: Int = x) {
    sb.appendLine(y)
}

fun box(): String {
    TestClass(5).foo()
    TestClass(6).bar()

    assertEquals("5\n6\n", sb.toString())
    return "OK"
}