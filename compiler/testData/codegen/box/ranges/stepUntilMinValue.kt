// IGNORE_BACKEND: JS
// WITH_STDLIB
// CHECK_BYTECODE_TEXT
// 5 INVOKESTATIC kotlin/ranges/RangesKt.step
// 4 INVOKESTATIC kotlin/ranges/URangesKt.step

fun box(): String {
    // Byte
    for (i in (0 until Byte.MIN_VALUE step 2)) {
        return "FAIL"
    }

    // UByte
    for (i in (0.toUByte() until UByte.MIN_VALUE step 2)) {
        return "FAIL"
    }

    // Short
    for (i in (0 until Short.MIN_VALUE step 2)) {
        return "FAIL"
    }

    // UShort
    for (i in (0.toUShort() until UShort.MIN_VALUE step 2)) {
        return "FAIL"
    }

    // Int
    for (i in (0 until Int.MIN_VALUE step 2)) {
        return "FAIL"
    }

    // UInt
    for (i in (0.toUInt() until UInt.MIN_VALUE step 2)) {
        return "FAIL"
    }

    // Long
    for (i in (0 until Long.MIN_VALUE step 2)) {
        return "FAIL"
    }

    // ULong
    for (i in (0UL until ULong.MIN_VALUE step 2L)) {
        return "FAIL"
    }

    // Char
    for (i in (Char.MIN_VALUE until Char.MIN_VALUE step 2)) {
        return "FAIL"
    }

    return "OK"
}