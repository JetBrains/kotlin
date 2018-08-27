/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.test

@Suppress("UNUSED_PARAMETER")
public fun assertTypeEquals(expected: Any?, actual: Any?) {
    //TODO: find analogue
    //assertEquals(expected?.javaClass, actual?.javaClass)
}

fun <T> Iterable<T>.assertSorted(isInOrder: (T, T) -> Boolean): Unit { this.iterator().assertSorted(isInOrder) }
fun <T> Iterator<T>.assertSorted(isInOrder: (T, T) -> Boolean) {
    if (!hasNext()) return
    var index = 0
    var prev = next()
    while (hasNext()) {
        index += 1
        val next = next()
        assertTrue(isInOrder(prev, next), "Not in order at position $index, element[${index-1}]: $prev, element[$index]: $next")
        prev = next
    }
    return
}
