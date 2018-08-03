// !WITH_BASIC_TYPES_PROVIDER

/*
 KOTLIN SPEC TEST (NEGATIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 3
 SENTENCE 2: Each entry consists of a boolean condition (or a special else condition), each of which is checked and evaluated in order of appearance.
 NUMBER: 1
 DESCRIPTION: 'When' without bound value and with not boolean condition in 'when condition'.
 */

// CASE DESCRIPTION: 'When' with numbers (Int, Short, Byte, Long, Float, Double) and Char (used as a number) in 'when condition'.
fun case_1(
    value1: Int,
    value2: Int,
    value3: Short,
    value4: Byte,
    value5: Char,
    value6: Long,
    value7: Float,
    value8: Double,
    value9: _BasicTypesProvider
): String {
    when {
        <!TYPE_MISMATCH!>-9 + 11<!> -> return ""
        <!TYPE_MISMATCH!>9 / 11<!> -> return ""
        <!TYPE_MISMATCH!>9 * value2<!> -> return ""
        <!TYPE_MISMATCH!>-9 % 11<!> -> return ""
        <!TYPE_MISMATCH!>9 or 11<!> -> return ""
        <!TYPE_MISMATCH!>-9 and 11<!> -> return ""
        <!TYPE_MISMATCH!>value1 xor 11<!> -> return ""
        <!TYPE_MISMATCH!>-0 shr 50<!> -> return ""
        <!TYPE_MISMATCH!>value1 ushr value2<!> -> return ""
        <!TYPE_MISMATCH!>9.inv()<!> -> return ""
        <!TYPE_MISMATCH!>9 % 11 + 123 - value1 or 11 and value2<!> -> return ""
        <!TYPE_MISMATCH!>value3 * 143.toShort()<!> -> return ""
        <!TYPE_MISMATCH!>getShort(13) - 143.toShort()<!> -> return ""
        <!TYPE_MISMATCH!>value4 * (-143).toByte()<!> -> return ""
        <!TYPE_MISMATCH!>value9.getByte(22) % 143.toByte()<!> -> return ""
        <!TYPE_MISMATCH!>value5 +10<!> -> return ""
        <!TYPE_MISMATCH!>143.toChar() - 11<!> -> return ""
        <!TYPE_MISMATCH!>value6 * 432L<!> -> return ""
        <!TYPE_MISMATCH!>-0L * 20L<!> -> return ""
        <!TYPE_MISMATCH!>value9.getLong(123) % 1234123513543L<!> -> return ""
        <!TYPE_MISMATCH!>-.012f / value7<!> -> return ""
        <!TYPE_MISMATCH!>value9.getLong(321) - 10000f<!> -> return ""
        <!TYPE_MISMATCH!>-.012 + value8<!> -> return ""
        <!TYPE_MISMATCH!>value9.getDouble(321) - 10000.0<!> -> return ""
        <!TYPE_MISMATCH!>9 % 11 + 123 - value1 or 11 and <!TYPE_MISMATCH!>value2 / value9.getDouble(321) - -.1223F<!><!> -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with String and Char in 'when condition'.
fun case_2(value1: String, value2: Char, value3: _BasicTypesProvider): String {
    when {
        <!TYPE_MISMATCH!>""<!> -> return ""
        <!CONSTANT_EXPECTED_TYPE_MISMATCH!>'-'<!> -> return ""
        <!TYPE_MISMATCH!>"$value1$value2"<!> -> return ""
        <!TYPE_MISMATCH!>value1<!> -> return ""
        <!TYPE_MISMATCH!>"$value1${getString(43)}"<!> -> return ""
        <!TYPE_MISMATCH!>"${value3.getString(33)}"<!> -> return ""
        <!TYPE_MISMATCH!>"${value3.getString(33)}"<!> -> return ""
        <!TYPE_MISMATCH!>getChar(32)<!> -> return ""
        <!TYPE_MISMATCH!>value3.getChar(32) - 20<!> -> return ""
        <!TYPE_MISMATCH!>value1 + "..." + value3.getString(43)<!> -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with Nothing in 'when condition'.
fun case_3(value1: Nothing, <!UNUSED_PARAMETER!>value2<!>: _BasicTypesProvider): String {
    when {
        value1 -> <!UNREACHABLE_CODE!>return ""<!>
        <!UNREACHABLE_CODE!>value2.getNothing() -> return ""<!>
        <!UNREACHABLE_CODE!>getNothing() -> return ""<!>
        <!UNREACHABLE_CODE!>throw Exception() -> return ""<!>
    }

    <!UNREACHABLE_CODE!>return ""<!>
}

// CASE DESCRIPTION: 'When' with Unit in 'when condition'.
fun case_4(value1: Unit, value2: _BasicTypesProvider): String {
    when {
        <!TYPE_MISMATCH!>value1<!> -> return ""
        <!TYPE_MISMATCH!>value2.getUnit()<!> -> return ""
        <!TYPE_MISMATCH!>getUnit()<!> -> return ""
        <!TYPE_MISMATCH!>{}<!> -> return ""
        <!TYPE_MISMATCH!>fun() {}<!> -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with Any in 'when condition'.
fun case_5(value1: Any, value2: _BasicTypesProvider): String {
    when {
        <!TYPE_MISMATCH!>value1<!> -> return ""
        <!TYPE_MISMATCH!>value2.getAny()<!> -> return ""
        <!TYPE_MISMATCH!>getAny()<!> -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with List (Collection example) in 'when condition'.
fun case_6(value1: List<Int>, value2: _BasicTypesProvider): String {
    when {
        <!TYPE_MISMATCH!>value1<!> -> return ""
        <!TYPE_MISMATCH!>value2.getList()<!> -> return ""
        <!TYPE_MISMATCH!>getList()<!> -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with range expression (IntRange) in 'when condition'.
fun case_7(value1: Int, value2: Int): String {
    when {
        <!TYPE_MISMATCH!>-10..-1<!> -> return ""
        <!TYPE_MISMATCH!>-0..0<!> -> return ""
        <!TYPE_MISMATCH!>1..value1<!> -> return ""
        <!TYPE_MISMATCH!>value1 + 1..value2<!> -> return ""
    }

    return ""
}