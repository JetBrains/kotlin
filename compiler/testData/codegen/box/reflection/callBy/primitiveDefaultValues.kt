// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.test.assertEquals

fun primitives(
        boolean: Boolean = true,
        character: Char = 'z',
        byte: Byte = 5.toByte(),
        short: Short = (-5).toShort(),
        int: Int = 2000000000,
        float: Float = -2.72f,
        long: Long = 1000000000000000000L,
        double: Double = 3.14159265359
) {
    assertEquals(true, boolean)
    assertEquals('z', character)
    assertEquals(5.toByte(), byte)
    assertEquals((-5).toShort(), short)
    assertEquals(2000000000, int)
    assertEquals(-2.72f, float)
    assertEquals(1000000000000000000L, long)
    assertEquals(3.14159265359, double)
}

fun box(): String {
    ::primitives.callBy(emptyMap())
    return "OK"
}
