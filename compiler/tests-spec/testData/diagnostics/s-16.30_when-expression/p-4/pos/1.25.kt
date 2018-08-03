// !WITH_BASIC_TYPES_PROVIDER

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 4
 SENTENCE 1: When expression with bound value (the form where the expression enclosed in parantheses is present) are very similar to the form without bound value, but use different syntax for conditions.
 NUMBER: 25
 DESCRIPTION: 'When' with bound value and a list of boolean conditions in 'when entry'.
 */

// CASE DESCRIPTION: 'When' with list of Integer.
fun case_1(value: Int, value7: _BasicTypesProvider): String {
    when (value) {
        21, 0, -1, 100000, Int.MAX_VALUE -> return ""
        -11111, value7.getInt(0), getInt(999) -> return ""
        value7.getInt(0), getInt(999) -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with list of Integer with 'else' branch.
fun case_2(value: Int, value7: _BasicTypesProvider): String = when (value) {
    21, 0, -1, 100000, Int.MAX_VALUE -> ""
    -11111, value7.getInt(0), getInt(999) -> ""
    value7.getInt(0), getInt(999) -> ""
    else -> ""
}

// CASE DESCRIPTION: 'When' with list of Short.
fun case_3(value: Short, value7: _BasicTypesProvider): String {
    when (value) {
        21.toShort(), 0.toShort(), (-1).toShort(), 100.toShort(), Short.MAX_VALUE -> return ""
        (-111).toShort(), value7.getShort(0), getShort(999) -> return ""
        value7.getShort(0), getShort(999) -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with list of Short with 'else' branch.
fun case_4(value: Short, value7: _BasicTypesProvider): String = when (value) {
    21.toShort(), 0.toShort(), (-1).toShort(), 100.toShort(), Short.MAX_VALUE -> ""
    (-111).toShort(), value7.getShort(0), getShort(999) -> ""
    value7.getShort(0), getShort(999) -> ""
    else -> ""
}

// CASE DESCRIPTION: 'When' with list of Long.
fun case_5(value: Long, value7: _BasicTypesProvider): String {
    when (value) {
        21L, 0L, -1L, 100L, Long.MAX_VALUE -> return ""
        -111L, value7.getLong(0), getLong(999) -> return ""
        value7.getLong(0), getLong(999) -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with list of Long with 'else' branch.
fun case_6(value: Long, value7: _BasicTypesProvider): String = when (value) {
    21L, 0L, -1L, 100L, Long.MAX_VALUE -> ""
    -111L, value7.getLong(0), getLong(999) -> ""
    value7.getLong(0), getLong(999) -> ""
    else -> ""
}

// CASE DESCRIPTION: 'When' with list of Float.
fun case_7(value: Float, value7: _BasicTypesProvider): String {
    when (value) {
        21.123124f, 0.1f, -0f, -.100F, Float.MIN_VALUE -> return ""
        -111f, value7.getFloat(10), getFloat(999) -> return ""
        value7.getFloat(0), getFloat(999) -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with list of Float with 'else' branch.
fun case_8(value: Float, value7: _BasicTypesProvider): String = when (value) {
    21.123124f, 0.1f, -0f, -.100F, Float.MIN_VALUE -> ""
    -111f, value7.getFloat(10), getFloat(999) -> ""
    value7.getFloat(0), getFloat(999) -> ""
    else -> ""
}

// CASE DESCRIPTION: 'When' with list of Double.
fun case_9(value: Double, value7: _BasicTypesProvider): String {
    when (value) {
        21.123124, 0.1, -0.0, -.100, Double.MIN_VALUE -> return ""
        -111.0, value7.getDouble(10), getDouble(999) -> return ""
        value7.getDouble(0), getDouble(999) -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with list of Double with 'else' branch.
fun case_10(value: Double, value7: _BasicTypesProvider): String = when (value) {
    21.123124, 0.1, -0.0, -.100, Double.MIN_VALUE -> ""
    -111.0, value7.getDouble(10), getDouble(999) -> ""
    value7.getDouble(0), getDouble(999) -> ""
    else -> ""
}

// CASE DESCRIPTION: 'When' with list of Byte.
fun case_11(value: Byte, value7: _BasicTypesProvider): String {
    when (value) {
        21.toByte(), 1.toByte(), 0.toByte(), (-100).toByte(), Byte.MIN_VALUE -> return ""
        (-111).toByte(), value7.getByte(10), getByte(999) -> return ""
        value7.getByte(0), getByte(999) -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with list of Byte with 'else' branch.
fun case_12(value: Byte, value7: _BasicTypesProvider): String = when (value) {
    21.toByte(), 1.toByte(), 0.toByte(), (-100).toByte(), Byte.MIN_VALUE -> ""
    (-111).toByte(), value7.getByte(10), getByte(999) -> ""
    value7.getByte(0), getByte(999) -> ""
    else -> ""
}

// CASE DESCRIPTION: 'When' with list of Char.
fun case_13(value: Char, value7: _BasicTypesProvider): String {
    when (value) {
        21.toChar(), 1.toChar(), '-', (-1030).toChar(), Char.MAX_LOW_SURROGATE -> return ""
        '.', '1', (-100).toChar(), Char.MIN_HIGH_SURROGATE -> return ""
        (-111).toChar(), value7.getChar(10), getChar(999) -> return ""
        value7.getChar(0), getChar(999), '?' -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with list of Char with 'else' branch.
fun case_14(value: Char, value7: _BasicTypesProvider): String = when (value) {
    21.toChar(), 1.toChar(), '-', (-1030).toChar(), Char.MAX_LOW_SURROGATE -> ""
    '.', '1', (-100).toChar(), Char.MIN_HIGH_SURROGATE -> ""
    (-111).toChar(), value7.getChar(10), getChar(999) -> ""
    value7.getChar(0), getChar(999), '?' -> ""
    else -> ""
}

// CASE DESCRIPTION: 'When' with list of String.
fun case_15(value: String, value7: _BasicTypesProvider): String {
    when (value) {
        "123123", "...", getString(44) -> return ""
        ".", "1", "-", value7.toString(), value7.getString(33333) -> return ""
        "-111", "......................................................." -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with list of String with 'else' branch.
fun case_16(value: String, value7: _BasicTypesProvider): String = when (value) {
    "123123", "...", getString(44) -> ""
    ".", "1", "-", value7.toString(), value7.getString(33333) -> ""
    "-111", "......................................................." -> ""
    else -> ""
}
