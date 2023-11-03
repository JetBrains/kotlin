// IGNORE_BACKEND: JS
// WITH_STDLIB
// CHECK_BYTECODE_TEXT
// 5 INVOKESTATIC kotlin/ranges/RangesKt.step
// 4 INVOKESTATIC kotlin/ranges/URangesKt.step

import kotlin.test.*

fun box(): String {
    // Byte
    val byteProgression = 0 until Byte.MIN_VALUE step 2
    assertTrue(byteProgression.isEmpty())

    // UByte
    val uByteProgression = 0.toUByte() until UByte.MIN_VALUE step 2
    assertTrue(uByteProgression.isEmpty())

    // Short
    val shorProgression = 0 until Short.MIN_VALUE step 2
    assertTrue(shorProgression.isEmpty())

    // UShort
    val uShortProgression = 0.toUShort() until UShort.MIN_VALUE step 2
    assertTrue(uShortProgression.isEmpty())

    // Int
    val intProgression = 0 until Int.MIN_VALUE step 2
    assertTrue(intProgression.isEmpty())

    // UInt
    val uIntProgression = 0.toUInt() until UInt.MIN_VALUE step 2
    assertTrue(uIntProgression.isEmpty())

    // Long
    val longProgression = 0 until Long.MIN_VALUE step 2
    assertTrue(longProgression.isEmpty())

    // ULong
    val uLongProgression = 0UL until ULong.MIN_VALUE step 2L
    assertTrue(uLongProgression.isEmpty())

    // Char
    val charProgression = Char.MIN_VALUE until Char.MIN_VALUE step 2
    assertTrue(charProgression.isEmpty())

    return "OK"
}