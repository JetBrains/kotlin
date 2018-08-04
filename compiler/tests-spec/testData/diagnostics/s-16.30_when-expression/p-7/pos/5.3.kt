// !DIAGNOSTICS: -DEPRECATED_IDENTITY_EQUALS

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 7
 SENTENCE 5: Any other expression.
 NUMBER: 3
 DESCRIPTION: 'When' with bound value and equality expressions in 'when condition'.
 */

// CASE DESCRIPTION: 'When' with boolean equality expression in 'when condition' and 'else' branch.
fun case_1(value: Boolean, flag1: Boolean, flag2: Boolean, obj1: List<String>, obj2: List<String>): String = when (value) {
    flag1 == flag2 -> ""
    flag1 === flag2 -> ""
    obj1 === obj2 -> ""
    else -> ""
}

// CASE DESCRIPTION: 'When' with boolean equality expression in 'when condition'.
fun case_2(value: Boolean, flag1: Boolean, flag2: Boolean, obj1: List<String>, obj2: List<String>): String {
    when (value) {
        flag1 == flag2 -> return ""
        flag1 === flag2 -> return ""
        obj1 === obj2 -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with boolean not equality expression in 'when condition'.
fun case_3(value: Boolean, flag1: Boolean, flag2: Boolean, obj1: List<String>, obj2: List<String>): String {
    when (value) {
        flag1 != flag2 -> return ""
        flag1 !== flag2 -> return ""
        obj1 !== obj2 -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with boolean not equality expression in 'when condition' and 'else' branch.
fun case_4(value: Boolean, flag1: Boolean, flag2: Boolean, obj1: List<String>, obj2: List<String>): String = when (value) {
    flag1 != flag2 -> ""
    flag1 !== flag2 -> ""
    obj1 !== obj2 -> ""
    else -> ""
}

// CASE DESCRIPTION: 'When' with Char and String equality expressions in 'when condition'.
fun case_5(value: Boolean, value1: Char, value2: String): String = when (value) {
    value1 == '.' -> ""
    value1 !== '-' -> ""
    value2 == "..." -> ""
    value2 != "" -> ""
    "zzz" !== "" -> ""
    '=' != 'z' -> ""
    '-' === '-' -> ""
    '=' !== 'z' -> ""
    else -> ""
}

// CASE DESCRIPTION: 'When' with numbers (and Char as number) equality expressions in 'when condition'.
fun case_6(value: Boolean, value1: Int, value2: Float, value3: Double, value4: Byte, value5: Char, value6: Short, value7: Long): String = when (value) {
    value1 == 9921 -> ""
    value1 != 212 -> ""
    value1 !== -1111111 -> ""
    900 == -10 -> ""
    900 === -10 -> ""

    value2 !== 11.4f -> ""
    value2 == -.4f -> ""
    value2 === 100000F -> ""
    0.133f !== .0132F -> ""
    0.900F !== -10f -> ""

    value3 == 3.11 -> ""
    value3 != 0.01 -> ""
    value3 !== 1.01 -> ""
    0.133 === .0132 -> ""
    0.900 !== -10.0 -> ""

    value4 == 100.toByte() -> ""
    value4 != 10.toByte() -> ""
    value4 !== 100L.toByte() -> ""
    90L.toByte() === 100.toByte() -> ""
    0L.toByte() === 88.toByte() -> ""

    value5 == 100.toChar() -> ""
    value5 != 10.toChar() -> ""
    value5 === 100L.toChar() -> ""
    90L.toChar() !== 100.toChar() -> ""
    0L.toChar() === 88.toChar() -> ""

    value6 == 100.toShort() -> ""
    value6 != 10.toShort() -> ""
    value6 === 100L.toShort() -> ""
    90L.toShort() !== 100.toShort() -> ""
    0L.toShort() === 88.toShort() -> ""

    value7 == 100L -> ""
    value7 != -10L -> ""
    value7 === -100L -> ""
    1902901293L != -9902901293L -> ""
    902901293L !== 3902901293L -> ""
}

// CASE DESCRIPTION: 'When' as expression with Boolean (literals) equality expressions in 'when condition'.
fun case_7(value: Boolean): String = when (value) {
    true || false == false || !false && true -> ""
    false || false != false || !!!!false && true -> ""
}

// CASE DESCRIPTION: 'When' as expression with Boolean (literals) equality expressions in 'when condition' and 'else' branch.
fun case_8(value: Boolean): String = when (value) {
    true || false == false || !false && true -> ""
    false || false != false || !!!!false && true -> ""
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> ""
}

// CASE DESCRIPTION: 'When' as statement with Boolean (literals) equality expressions in 'when condition'.
fun case_9(value: Boolean): String {
    <!DEBUG_INFO_IMPLICIT_EXHAUSTIVE!>when (value) {
        true || false == false || !false && true -> return ""
        false || false != false || !!!!false && true -> return ""
    }<!>

    <!UNREACHABLE_CODE!>return ""<!>
}

// CASE DESCRIPTION: 'When' as expression with Boolean (variables and literals) equality expressions in 'when condition' and 'else' branch.
fun case_10(value: Boolean, value1: Boolean, value2: Boolean): String = when (value) {
    value1 == false || !false && true -> ""
    value2 != false || !!!!false && true -> ""
    value2 !== !false || !!!false && true -> ""
    value1 === !!false || !(false && true) -> ""
    else -> ""
}