// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.test.assertEquals

fun String.sum(other: String = "b") = this + other

fun box(): String {
    val f = String::sum
    assertEquals("ab", f.callBy(mapOf(f.parameters.first() to "a")))
    return "OK"
}
