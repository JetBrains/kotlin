/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 7
 SENTENCE 5: Any other expression.
 NUMBER: 9
 DESCRIPTION: 'When' with bound value and elvis operator expressions in 'when condition'.
 */

// CASE DESCRIPTION: 'When' with 'else' branch (as expression) and several entries.
fun case_1(value: Boolean, value1: Int?): String = when (value) {
    value1 ?: true -> ""
    value1!! > 100 -> ""
    value1 <!USELESS_ELVIS!>?: false<!> -> ""
    else -> ""
}

// CASE DESCRIPTION: 'When' without 'else' branch (as statement) and several entries.
fun case_2(value: Boolean, value1: Int?): String {
    when (value) {
        value1 ?: true -> return ""
        value1!! > 100 -> return ""
        value1 <!USELESS_ELVIS!>?: false<!> -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with 'else' branch (as expression) and only one entry.
fun case_3(value: Boolean, value1: Int?): String = when (value) {
    value1 ?: true -> ""
    else -> ""
}

// CASE DESCRIPTION: 'When' without 'else' branch (as statement) and only one entry.
fun case_4(value: Boolean, value1: Int?): String {
    when (value) {
        value1 ?: true -> return ""
    }

    return ""
}