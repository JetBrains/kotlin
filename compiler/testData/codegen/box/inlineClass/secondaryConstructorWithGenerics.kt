// WITH_STDLIB

import kotlin.test.*

// Based on KT-42649.
inline class IC<T>(val value: List<T>) {
    constructor(value: T) : this(listOf(value))
}

fun box(): String {
    assertEquals("abc", IC("abc").value.singleOrNull())

    return "OK"
}
