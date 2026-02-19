// WITH_STDLIB

import kotlin.test.*

enum class E {
    E3,
    E1,
    E2
}

fun box(): String {
    assertEquals("E1", E.valueOf("E1").toString())
    assertEquals("E2", E.valueOf("E2").toString())
    assertEquals("E3", E.valueOf("E3").toString())
    assertEquals("E1", enumValueOf<E>("E1").toString())
    assertEquals("E2", enumValueOf<E>("E2").toString())
    assertEquals("E3", enumValueOf<E>("E3").toString())

    return "OK"
}
