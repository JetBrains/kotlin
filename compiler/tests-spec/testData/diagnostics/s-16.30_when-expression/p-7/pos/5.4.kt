/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 7
 SENTENCE 5: Any other expression.
 NUMBER: 4
 DESCRIPTION: 'When' with bound value and comparison expressions in 'when condition'.
 */

// CASE DESCRIPTION: 'When' with comparison expression (variables and literals) in 'when condition' and 'else' branch.
fun case_1(value: Boolean, value1: Int, value2: Int): String = when (value) {
    value1 > 900 -> ""
    value2 < 900 && value1 >= 900 -> ""
    value2 <= 1800 && value1 >= 400 -> ""
    else -> ""
}

// CASE DESCRIPTION: 'When' with comparison expression (variables and literals) in 'when condition'.
fun case_2(value: Boolean, value1: Int, value2: Int): String {
    when (value) {
        value1 > 900 -> return ""
        value2 < 900 && value1 >= 900 -> return ""
        value2 <= 1800 && value1 >= 400 -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with comparison expression (literals) in 'when condition'.
fun case_3(value: Boolean): String = when (value) {
    1100 > 900 -> ""
    9 < 900 && 111 >= 900 -> ""
}

// CASE DESCRIPTION: 'When' with comparison expression (literals) in 'when condition' and 'else' branch.
fun case_4(value: Boolean): String = when (value) {
    1100 > 900 -> ""
    9 < 900 && 111 >= 900 -> ""
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> ""
}

// CASE DESCRIPTION: 'When' as statement with comparison expression in 'when condition' (literals) and only one boolean value covered.
fun case_5(value: Boolean): String {
    when (value) {
        1100 > 900 -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' as statement with comparison expression in 'when condition' (literals) and only one boolean value covered and 'else' branch.
fun case_6(value: Boolean): String {
    <!DEBUG_INFO_IMPLICIT_EXHAUSTIVE!>when (value) {
        1100 > 900 -> return ""
        9 < 900 && 111 >= 900 -> return ""
    }<!>
}

// CASE DESCRIPTION: 'When' as statement with comparison expression in 'when condition' (literals) and both boolean value covered and 'else' branch (redundant).
fun case_7(value: Boolean): String {
    when (value) {
        1100 > 900 -> return ""
        9 < 900 && 111 >= 900 -> return ""
        else -> return ""
    }

    <!UNREACHABLE_CODE!>return ""<!>
}