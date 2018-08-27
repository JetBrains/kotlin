/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.inline.inline8

import kotlin.test.*

@Suppress("NOTHING_TO_INLINE")
inline fun foo(i3: Int, i4: Int) : Int {
    return i3 + i3 + i4
}

fun bar(i1: Int, i2: Int) : Int {
    return foo(i1 + i2, 2)
}

@Test fun runTest() {
    println(bar(1, 2).toString())
}
