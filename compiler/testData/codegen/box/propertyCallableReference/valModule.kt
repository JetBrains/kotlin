// WITH_STDLIB

import kotlin.test.*

val x = 42

fun box(): String {
    val p = ::x
    assertEquals(42, p.get())

    return "OK"
}
