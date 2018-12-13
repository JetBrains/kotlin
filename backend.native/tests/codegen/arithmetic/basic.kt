package codegen.arithmetic.basic

import kotlin.test.*

// Check that compiler doesn't optimize it to `true`
fun selfCmp1(x: Int) = x + 1 > x

fun selfCmp2(x: Int) = x - 1 < x

@Test
fun selfComparison() {
    assertFalse(selfCmp1(Int.MAX_VALUE))
    assertFalse(selfCmp2(Int.MIN_VALUE))
}

private fun charCornersMinus(): Int {
    val a: Char = 0xFFFF.toChar()
    val b: Char = 0.toChar()
    return a - b
}

private fun charCornersComparison(): Boolean {
    val a = 0xFFFF.toChar()
    val b = 0.toChar()
    return a < b
}

@Test
fun charCornerCases() {
    assertEquals(65535, charCornersMinus())
    assertFalse(charCornersComparison())
}

@Test
fun shifts() {
    assertEquals(-2147483648, 1 shl -1)
    assertEquals(0, 1 shr -1)
    assertEquals(1, 1 shl 32)
    assertEquals(1073741823, -1 ushr 2)
    assertEquals(-1, -1 shr 2)
}

@Test
@kotlin.ExperimentalUnsignedTypes
fun uintTests() {
    assertEquals(UInt.MAX_VALUE, UInt.MIN_VALUE - 1u)
}

@Test
fun charConversions() {
    assertEquals(97.0, 'a'.toDouble())
    assertEquals(-1, Char.MAX_VALUE.toShort())
    assertEquals(32768, Short.MIN_VALUE.toChar().toInt())
    assertEquals(-1, Char.MAX_VALUE.toByte())
    assertEquals(65408, Byte.MIN_VALUE.toChar().toInt())
    assertEquals(0, Float.MIN_VALUE.toChar().toInt())
}

@Test
fun doubleBasic() {
    assertEquals(1, 0f.compareTo(-0f))
    assertEquals(1, 0.0.compareTo(-0.0))

    assertEquals(1.0, Double.fromBits(1.0.toBits()))
    assertEquals(1.0f, Float.fromBits(1.0f.toBits()))

    assertEquals(Double.NaN, Double.fromBits((0 / 0.0).toBits()))
    assertEquals(Float.NaN, Float.fromBits((0 / 0f).toBits()))
}

@Test
fun integralToFloat() {
    assertEquals(9.223372E18f, Long.MAX_VALUE.toFloat())
    assertEquals(-9.223372E18f, Long.MIN_VALUE.toFloat())

    assertEquals(-2.147483648E9, Int.MIN_VALUE.toDouble())
    assertEquals(2.147483647E9, Int.MAX_VALUE.toDouble())

    assertEquals(2147483647, Double.MAX_VALUE.toInt())
    assertEquals(0, Float.MIN_VALUE.toLong())

    assertEquals(9223372036854775807, Float.MAX_VALUE.toLong())
    assertEquals(0, Double.MIN_VALUE.toInt())
}

@Test
fun compareIntToFloat() {
    assertEquals(1, 0.compareTo(-0.0f))
    assertEquals(0, 0.compareTo(+0.0f))
}