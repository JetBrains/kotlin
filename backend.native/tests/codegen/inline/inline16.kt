package codegen.inline.inline16

import kotlin.test.*

@Suppress("NOTHING_TO_INLINE")
inline fun <T, C : MutableCollection<in T>> foo(destination: C, predicate: (T) -> Boolean): C {
    for (element in destination) {
        val value = element as T
        if (!predicate(value)) destination.add(value)
    }
    return destination
}

fun bar(): Boolean {
    val result = foo <Int, MutableList<Int>> (mutableListOf(1, 2, 3)) { true }
    return result.isEmpty()
}

@Test fun runTest() {
    println(bar().toString())
}
