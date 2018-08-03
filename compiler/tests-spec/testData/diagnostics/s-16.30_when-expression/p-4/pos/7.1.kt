// !WITH_BASIC_TYPES_PROVIDER

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 4
 SENTENCE 7: Any other expression.
 NUMBER: 1
 DESCRIPTION: 'When' with different variants of the arithmetic expressions (additive expression and multiplicative expression) in 'when condition'.
 */

// CASE DESCRIPTION: 'When' with 'when condition' as arithmetic expression with Short.
fun case_1(value: Short, value1: Short, value2: _BasicTypesProvider): String {
    val value3 = 912.toShort()

    when (value) {
        2.toShort() -> return ""
        (2.toShort() + 2.toShort()).toShort() -> return ""
        (2.toShort() * 6.toShort()).toShort() -> return ""
        (8.toShort() / 5.toShort()).toShort() -> return ""
        (8.toShort() % 5.toShort()).toShort() -> return ""
        (9.toShort() - 1.toShort()).toShort() -> return ""
        (2.toShort() + value3 * 2 / 2 % 2 - 2).toShort() -> return ""
        Int.MIN_VALUE.inv().toShort() -> return ""
        Int.MAX_VALUE.hashCode().inv().toShort() -> return ""
        (value1 * value3).toShort() -> return ""
        (value1 * 2.toShort() / 10.toShort() + 5.toShort() + 14.toShort() / getShort(1000) % 4.toShort() * value2.getShort(1000)).toShort() -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with 'when condition' as arithmetic expression with Short, and 'else' branch.
fun case_2(value: Short, value1: Short, value2: _BasicTypesProvider): String {
    val value3 = 912.toShort()

    return when (value) {
        2.toShort() -> ""
        (2.toShort() + 2.toShort()).toShort() -> ""
        (2.toShort() * 6.toShort()).toShort() -> ""
        (8.toShort() / 5.toShort()).toShort() -> ""
        (8.toShort() % 5.toShort()).toShort() -> ""
        (9.toShort() - 1.toShort()).toShort() -> ""
        (2.toShort() + value3 * 2 / 2 % 2 - 2).toShort() -> ""
        Int.MIN_VALUE.inv().toShort() -> ""
        Int.MAX_VALUE.hashCode().inv().toShort() -> ""
        (value1 * value3).toShort() -> ""
        (value1 * 2.toShort() / 10.toShort() + 5.toShort() + 14.toShort() / getShort(1000) % 4.toShort() * value2.getShort(1000)).toShort() -> ""
        else -> ""
    }
}

// CASE DESCRIPTION: 'When' with 'when condition' as arithmetic expression with Int.
fun case_3(value: Int, value1: Int, value2: _BasicTypesProvider): String {
    val value3 = 912

    when (value) {
        2 -> return ""
        2 + 2 -> return ""
        2 * 3 -> return ""
        8 / 1 -> return ""
        8 % 5 -> return ""
        4 - 3 -> return ""
        2 + value3 * 2 / 2 % 2 - 2 -> return ""
        32 shl value3 -> return ""
        value1 shr value2.getInt(1000) -> return ""
        64 ushr getInt(1000) -> return ""
        value2.getInt(1000) and 4 -> return ""
        16 or 5 -> return ""
        value1 xor 55 -> return ""
        55.inv() -> return ""
        Int.MIN_VALUE.inv() -> return ""
        Int.MAX_VALUE.hashCode().inv() -> return ""
        value1 * value3 -> return ""
        value1 * 2 / 10 or 5 + 14 / getInt(1000) % 4 ushr value2.getInt(1000) -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with 'when condition' as arithmetic expression with Int, and 'else' branch.
fun case_4(value: Int, value1: Int, value2: _BasicTypesProvider): String {
    val value3 = 912

    return when (value) {
        2 -> ""
        2 + 2 -> ""
        2 * 3 -> ""
        8 / 1 -> ""
        8 % 5 -> ""
        4 - 3 -> ""
        2 + value3 * 2 / 2 % 2 - 2 -> ""
        32 shl value3 -> ""
        value1 shr value2.getInt(1000) -> ""
        64 ushr getInt(1000) -> ""
        value2.getInt(1000) and 4 -> ""
        16 or 5 -> ""
        value1 xor 55 -> ""
        55.inv() -> ""
        Int.MIN_VALUE.inv() -> ""
        Int.MAX_VALUE.hashCode().inv() -> ""
        value1 * value3 -> ""
        value1 * 2 / 10 or 5 + 14 / getInt(1000) % 4 ushr value2.getInt(1000) -> ""
        else -> ""
    }
}

// CASE DESCRIPTION: 'When' with 'when condition' as arithmetic expression with Float.
fun case_5(value: Float, value1: Float, value2: _BasicTypesProvider): String {
    val value3 = 912.113f

    when (value) {
        2f -> return ""
        2.1f + 2.9F -> return ""
        94.1243235235f * .9193f -> return ""
        -.000001f / 3F -> return ""
        8F % 3.1f -> return ""
        4.0F - 0.2f -> return ""
        2.111111f + value3 * 11f / 0.113F % 0.1F - 2.0f -> return ""
        value1 * getFloat(-100) -> return ""
        value1 * 2 / 14 / getFloat(1000) % 4 - value2.getFloat(1000) -> return ""
    }
 
    return ""
}

// CASE DESCRIPTION: 'When' with 'when condition' as arithmetic expression with Float, and 'else' branch.
fun case_6(value: Float, value1: Float, value2: _BasicTypesProvider): String {
    val value3 = 912.113f

    return when (value) {
        2f -> ""
        2.1f + 2.9F -> ""
        94.1243235235f * .9193f -> ""
        -.000001f / 3F -> ""
        8F % 3.1f -> ""
        4.0F - 0.2f -> ""
        2.111111f + value3 * 11f / 0.113F % 0.1F - 2.0f -> ""
        value1 * getFloat(-100) -> ""
        value1 * 2 / 14 / getFloat(1000) % 4 - value2.getFloat(1000) -> ""
        else -> ""
    }
}

// CASE DESCRIPTION: 'When' with 'when condition' as arithmetic expression with Double.
fun case_7(value: Double, value1: Double, value2: _BasicTypesProvider): String {
    val value3 = 912.113

    when (value) {
        2.4 -> return ""
        2.1 + 2.9 -> return ""
        94.1243235235 * .9193 -> return ""
        -.000001 / 3.0 -> return ""
        8.0 % 3.0 -> return ""
        4.0 - 0.2 -> return ""
        2.111111 + value3 * 11.1 / 0.113 % 0.1 - 2.0 -> return ""
        value1 * getDouble(-100) -> return ""
        value1 * 2.4 / 14.0 / getDouble(1000) % 4.0 * value2.getDouble(1000) -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with 'when condition' as arithmetic expression with Double, and 'else' branch.
fun case_8(value: Double, value1: Double, value2: _BasicTypesProvider): String {
    val value3 = 912.113

    return when (value) {
        2.4 -> ""
        2.1 + 2.9 -> ""
        94.1243235235 * .9193 -> ""
        -.000001 / 3.0 -> ""
        8.0 % 3.0 -> ""
        4.0 - 0.2 -> ""
        2.111111 + value3 * 11.1 / 0.113 % 0.1 - 2.0 -> ""
        value1 * getDouble(-100) -> ""
        value1 * 2.4 / 14.0 / getDouble(1000) % 4.0 * value2.getDouble(1000) -> ""
        else -> ""
    }
}

// CASE DESCRIPTION: 'When' with 'when condition' as arithmetic expression with Byte.
fun case_9(value: Byte, value1: Byte, value2: _BasicTypesProvider): String {
    val value3 = 912.toByte()

    when (value) {
        2.toByte() -> return ""
        (2.toByte() + 2.toByte()).toByte() -> return ""
        (2.toByte() * 6.toByte()).toByte() -> return ""
        (8.toByte() / 5.toByte()).toByte() -> return ""
        (8.toByte() % 5.toByte()).toByte() -> return ""
        (9.toByte() - 1.toByte()).toByte() -> return ""
        (2.toByte() + value3 * 2 / 2 % 2 - 2).toByte() -> return ""
        Int.MIN_VALUE.inv().toByte() -> return ""
        Int.MAX_VALUE.hashCode().inv().toByte() -> return ""
        (value1 * value3).toByte() -> return ""
        (value1 * 2.toByte() / 10.toByte() - 5.toByte() + 14.toByte() / getByte(1000) % 4.toByte() * value2.getByte(1000)).toByte() -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with 'when condition' as arithmetic expression with Byte, and 'else' branch.
fun case_10(value: Byte, value1: Byte, value2: _BasicTypesProvider): String {
    val value3 = 912.toByte()

    return when (value) {
        2.toByte() -> ""
        (2.toByte() + 2.toByte()).toByte() -> ""
        (2.toByte() * 6.toByte()).toByte() -> ""
        (8.toByte() / 5.toByte()).toByte() -> ""
        (8.toByte() % 5.toByte()).toByte() -> ""
        (9.toByte() - 1.toByte()).toByte() -> ""
        (2.toByte() + value3 * 2 / 2 % 2 - 2).toByte() -> ""
        Int.MIN_VALUE.inv().toByte() -> ""
        Int.MAX_VALUE.hashCode().inv().toByte() -> ""
        (value1 * value3).toByte() -> ""
        (value1 * 2.toByte() / 10.toByte() - 5.toByte() + 14.toByte() / getByte(1000) % 4.toByte() * value2.getByte(1000)).toByte() -> ""
        else -> ""
    }
}

// CASE DESCRIPTION: 'When' with 'when condition' as arithmetic expression (minus and plus of Integer) with Char.
fun case_11(value: Char): String {
    when (value) {
        2.toChar() -> return ""
        2.toChar() + 2 -> return ""
        8.toChar() - 2 -> return ""
        Int.MIN_VALUE.toChar() -> return ""
        Int.MAX_VALUE.hashCode().toChar() -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with 'when condition' as arithmetic expression (minus and plus of Integer) with Char, and 'else' branch.
fun case_12(value: Char): String = when (value) {
    2.toChar() -> ""
    2.toChar() + 23 -> ""
    8.toChar() - 12 -> ""
    Int.MIN_VALUE.toChar() -> ""
    Int.MAX_VALUE.hashCode().toChar() -> ""
    else -> ""
}

// CASE DESCRIPTION: 'When' with 'when condition' as arithmetic expression with Long.
fun case_13(value: Long, value1: Long, value2: _BasicTypesProvider): String {
    val value3: Long = 34939942345L

    when (value) {
        11345342345L -> return ""
        2L + 2L -> return ""
        212452342345L * 2L -> return ""
        8L / 3L -> return ""
        8L % 5L -> return ""
        5323452342345L - 2L -> return ""
        159345342345L + value3 * 2L / 65939942345L % 2L - 85939942345L -> return ""
        32L shl value3.toInt() -> return ""
        value1 shr value2.getLong(1000).toInt() -> return ""
        64L ushr getLong(1000).toInt() -> return ""
        value2.getLong(1000) and 4 -> return ""
        33244523442345L or 5L -> return ""
        value1 xor 932452342345L -> return ""
        Int.MIN_VALUE.toLong() -> return ""
        Int.MAX_VALUE.hashCode().toLong() -> return ""
        value1 * value3 -> return ""
        value1 * 2L / 10L or 85939942345L + 14L / getLong(1000) % 4L ushr value2.getLong(1000).toInt() -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with 'when condition' as arithmetic expression with Long, and 'else' branch.
fun case_14(value: Long, value1: Long, value2: _BasicTypesProvider): String {
    val value3: Long = 34939942345L

    return when (value) {
        11345342345L -> ""
        2L + 2L -> ""
        212452342345L * 2L -> ""
        8L / 3L -> ""
        8L % 5L -> ""
        5323452342345L - 2L -> ""
        159345342345L + value3 * 2L / 65939942345L % 2L - 85939942345L -> ""
        32L shl value3.toInt() -> ""
        value1 shr value2.getLong(1000).toInt() -> ""
        64L ushr getLong(1000).toInt() -> ""
        value2.getLong(1000) and 4 -> ""
        33244523442345L or 5L -> ""
        value1 xor 932452342345L -> ""
        Int.MIN_VALUE.toLong() -> ""
        Int.MAX_VALUE.hashCode().toLong() -> ""
        value1 * value3 -> ""
        value1 * 2L / 10L or 85939942345L + 14L / getLong(1000) % 4L ushr value2.getLong(1000).toInt() -> ""
        else -> ""
    }
}