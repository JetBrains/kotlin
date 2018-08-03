// !WITH_BASIC_TYPES

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 3
 SENTENCE 2: Each entry consists of a boolean condition (or a special else condition), each of which is checked and evaluated in order of appearance.
 NUMBER: 1
 DESCRIPTION: 'When' without bound value and different variants of the boolean conditions (numbers and Char).
 */

// CASE DESCRIPTION: 'When' without 'else' branch.
fun case_1(
    value1: Int,
    value2: Float,
    value3: Double,
    value4: Short,
    value5: Byte,
    value6: Long,
    value7: _BasicTypesProvider,
    value8: Char
): String {
    when {
        value1 == 21 -> return ""
        value2 > -.000000001 && value2 < 0.000000001 -> return ""
        value3 > 2.0 && value3 <= 1000.90 -> return ""
        value4 == 0.toShort() -> return ""
        value5 > -128 || value5 < 127 -> return ""
        value6 > 213412341234L && value6 <= 1100000000000L || value6 == 0L -> return ""
        getInt('a') > 100 || getInt('+') < 10 -> return ""
        value7.getInt(-.00000001f) <= 100 || value7.getInt(4412.11F) >= 10 -> return ""
        getBoolean(0) || getBoolean(0L) -> return ""
        value7.getBoolean(0) && value7.getBoolean(0L) -> return ""
        11 == 11 || 13123123123123L == 0L || 0f == 0f && !!!(-.0000000001 == -.0000000001) || ((-10).toByte() == 90.toByte()) || 91.toChar() == 127.toChar() -> return ""
        value8 == 127.toChar() -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with 'else' branch.
fun case_2(
    value1: Int,
    value2: Float,
    value3: Double,
    value4: Short,
    value5: Byte,
    value6: Long,
    value7: _BasicTypesProvider,
    value8: Char
): String {
    return when {
        value1 == 21 -> ""
        value2 > -.000000001 && value2 < 0.000000001 -> ""
        value3 > 2.0 && value3 <= 1000.90 -> ""
        value4 == 0.toShort() -> ""
        value5 > -128 || value5 < 127 -> ""
        value6 > 213412341234L && value6 <= 1100000000000L || value6 == 0L -> ""
        getInt('a') > 100 || getInt('+') < 10 -> return ""
        value7.getInt(-.00000001f) <= 100 || value7.getInt(4412.11F) >= 10 -> return ""
        getBoolean(0) || getBoolean(0L) -> return ""
        value7.getBoolean(0) && value7.getBoolean(0L) -> return ""
        11 == 11 || 13123123123123L == 0L || 0f == 0f && !!!(-.0000000001 == -.0000000001) || ((-10).toByte() == 90.toByte()) || 91.toChar() == 127.toChar() -> return ""
        value8 == 127.toChar() -> return ""
        else -> ""
    }
}

// CASE DESCRIPTION: 'When' with only one 'else' branch.
fun case_3(): String = when {
    else -> ""
}