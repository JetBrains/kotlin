// WITH_REFLECT

import kotlin.test.assertEquals

data class Box<T>(val element: T)

fun box(): String {
    val p = Box<String>::element
    assertEquals("val Box<T>.element: T", p.toString())
    return p.call(Box("OK"))
}
