// WITH_STDLIB

import kotlin.test.*

var x = 42

fun box(): String {
    val p = ::x
    p.set(117)
    assertEquals(117, x)
    assertEquals(117, p.get())

    return "OK"
}
