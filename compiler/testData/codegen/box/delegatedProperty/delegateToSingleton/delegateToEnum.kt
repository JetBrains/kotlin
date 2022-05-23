// WITH_STDLIB
// WITH_REFLECT

import kotlin.test.assertEquals

enum class E {
    OK, NOT_OK
}

operator fun E.getValue(x: Any?, y: Any?): String = name

val s: String by E.OK

fun box(): String {
    assertEquals(E.OK, ::s.getDelegate())
    return s
}