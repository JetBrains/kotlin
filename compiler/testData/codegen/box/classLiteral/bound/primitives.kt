// IGNORE_BACKEND: JS_IR

// WITH_RUNTIME

import kotlin.test.assertEquals

fun box(): String {
    assertEquals(true::class, Boolean::class)
    assertEquals(42.toByte()::class, Byte::class)
    assertEquals('z'::class, Char::class)
    assertEquals(3.14::class, Double::class)
    assertEquals(2.72f::class, Float::class)
    assertEquals(42::class, Int::class)
    assertEquals(42L::class, Long::class)
    assertEquals(42.toShort()::class, Short::class)

    return "OK"
}
