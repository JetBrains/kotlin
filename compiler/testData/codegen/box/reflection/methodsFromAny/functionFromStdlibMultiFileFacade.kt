// IGNORE_BACKEND: JS_IR
// KT-12630 KotlinReflectionInternalError on referencing some functions from stdlib

// IGNORE_BACKEND: JS, NATIVE
// WITH_REFLECT

import kotlin.test.*

fun box(): String {
    val asIterable = List<Int>::asIterable
    assertEquals("fun kotlin.collections.Iterable<T>.asIterable(): kotlin.collections.Iterable<T>", asIterable.toString())

    return "OK"
}
