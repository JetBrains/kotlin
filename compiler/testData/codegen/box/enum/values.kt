// WITH_STDLIB

import kotlin.test.*

enum class E {
    E3,
    E1,
    E2
}

fun box(): String {
    assertEquals("E3", E.values()[0].toString())
    assertEquals("E1", E.values()[1].toString())
    assertEquals("E2", E.values()[2].toString())
    assertEquals("E3", enumValues<E>()[0].toString())
    assertEquals("E1", enumValues<E>()[1].toString())
    assertEquals("E2", enumValues<E>()[2].toString())

    return "OK"
}
