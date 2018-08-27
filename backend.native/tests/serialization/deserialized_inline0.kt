/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package serialization.deserialized_inline0

import kotlin.test.*


fun inline_todo() {
    try {
        TODO("OK")
    } catch (e: Throwable) {
        println(e.message)
    }
}

fun inline_maxof() {
    println(maxOf(10, 17))
    println(maxOf(17, 13))
    println(maxOf(17, 17))
}

fun inline_assert() {
    //assert(true)
}

@Test fun runTest() {
    inline_todo()
    inline_assert()
    inline_maxof()
}

