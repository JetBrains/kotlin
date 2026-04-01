// WITH_STDLIB
// WITH_REFLECT
// FILE: lib.kt
import kotlin.test.assertEquals

inline fun <reified T> bar(f: (T) -> Unit, tType: String): T? {
    assertEquals(tType, T::class.simpleName)
    return null
}

// FILE: main.kt

import kotlin.test.assertEquals

fun foo(x: Int?) {}
fun foo(y: String?) {}
fun foo(z: Boolean) {}

fun box(): String {
    val a1: Int? = bar(::foo, "Int")
    val a2: String? = bar(::foo, "String")
    val a3: Boolean? = bar<Boolean>(::foo, "Boolean")

    return "OK"
}
