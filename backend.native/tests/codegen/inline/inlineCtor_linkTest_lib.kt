/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package a

fun foo(n: Int, block: (Int) -> Int): Int {
    val arr = IntArray(n) { block(it) }
    var sum = 0
    for (x in arr) sum += x
    return sum
}