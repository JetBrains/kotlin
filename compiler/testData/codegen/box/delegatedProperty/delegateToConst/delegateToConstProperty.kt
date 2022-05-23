// WITH_STDLIB
// WITH_REFLECT

import kotlin.test.assertEquals

operator fun Any?.getValue(x: Any?, y: Any?): String {
    return "OK"
}
const val a = false

val s: String by a

fun box(): String {
    assertEquals(false, ::s.getDelegate())
    return s
}