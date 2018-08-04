/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 7
 SENTENCE 5: Any other expression.
 NUMBER: 13
 DESCRIPTION: 'When' with bound value and postfix operator expressions in 'when condition'.
 */

// CASE DESCRIPTION: 'When' with 'else' branch (as expression) and increment/decrement operator.
fun case_1(value: Int, value1: Int, value2: Int): String {
    var value1Mutable = value1
    var value2Mutable = value2

    return when (value) {
        <!UNUSED_CHANGED_VALUE!>value1Mutable++<!> -> ""
        <!UNUSED_CHANGED_VALUE!>value2Mutable--<!> -> ""
        else -> ""
    }
}

// CASE DESCRIPTION: 'When' without 'else' branch (as statement) and increment/decrement operator.
fun case_2(value: Int, value1: Int, value2: Int): String {
    var value1Mutable = value1
    var value2Mutable = value2

    when (value) {
        <!UNUSED_CHANGED_VALUE!>value1Mutable++<!> -> return ""
        <!UNUSED_CHANGED_VALUE!>value2Mutable--<!> -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with 'else' branch (as expression) and not-null assertion operator.
fun case_3(value: Boolean, value1: Boolean?, value2: Boolean?): String {
    return when (value) {
        !value1!! -> ""
        !value2!! -> ""
        value1 -> ""
        else -> ""
    }
}

// CASE DESCRIPTION: 'When' without 'else' branch (as statement) and not-null assertion operator.
fun case_4(value: Boolean, value1: Boolean?, value2: Boolean?): String {
    when (value) {
        !value1!! -> return ""
        !value2!! -> return ""
        value1 -> return ""
    }

    return ""
}