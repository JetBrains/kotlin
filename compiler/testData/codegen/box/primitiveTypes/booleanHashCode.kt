// WITH_STDLIB

import kotlin.test.*

fun box() : String {
    assertEquals(1231, true.hashCode())
    assertEquals(1231, hashCode(true))
    assertEquals(1237, false.hashCode())
    assertEquals(1237, hashCode(false))

    var b: Boolean? = null
    assertEquals(0, b.hashCode())
    assertEquals(0, hashCode(b))
    b = true
    assertEquals(1231, b.hashCode())
    assertEquals(1231, hashCode(b))
    b = false
    assertEquals(1237, b.hashCode())
    assertEquals(1237, hashCode(b))

    return "OK"
}

fun hashCode(b: Boolean?): Int {
    return b.hashCode()
}