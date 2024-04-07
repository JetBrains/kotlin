/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// JVM_ABI_K1_K2_DIFF: KT-63864
// WITH_STDLIB

import kotlin.test.*

class A {
    lateinit var s: String

    fun foo() = s
}

val sb = StringBuilder()

fun box(): String {
    val a = A()
    try {
        sb.appendLine(a.foo())
    }
    catch (e: RuntimeException) {
        sb.append("OK")
        return sb.toString()
    }
    return "Fail"
}