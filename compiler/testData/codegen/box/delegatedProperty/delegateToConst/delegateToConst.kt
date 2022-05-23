// WITH_STDLIB
// WITH_REFLECT

import kotlin.test.assertEquals

operator fun Any?.getValue(x: Any?, y: Any?): String {
    return "OK"
}

val s: String by 1

fun box(): String {
    assertEquals(1, ::s.getDelegate())
    return s
}