/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// JVM_ABI_K1_K2_DIFF: KT-63864
// WITH_STDLIB
import kotlin.test.*

val sb = StringBuilder()

class TestClass {
    companion object {
        init {
            sb.append("OK")
        }
    }
}

fun box(): String {
    val t1 = TestClass()
    val t2 = TestClass()

    return sb.toString()
}