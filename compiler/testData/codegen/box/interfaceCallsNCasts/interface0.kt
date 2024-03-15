/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// JVM_ABI_K1_K2_DIFF: KT-63864
// WITH_STDLIB
import kotlin.test.*

interface A {
    fun b() = c()
    fun c()
}

val sb = StringBuilder()

class B(): A {
    override fun c() {
        sb.append("OK")
    }
}

fun box(): String {
    val a:A = B()
    a.b()

    return sb.toString()
}
