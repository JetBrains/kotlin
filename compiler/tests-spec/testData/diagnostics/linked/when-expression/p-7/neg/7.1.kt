// !DIAGNOSTICS: -UNUSED_EXPRESSION

/*
 KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)

 SECTIONS: when-expression
 PARAGRAPH: 7
 SENTENCE: [7] The else condition, which works the exact same way as it would in the form without bound expression.
 NUMBER: 1
 DESCRIPTION: 'When' with bound value and with else branch not in the last position.
 */

// CASE DESCRIPTION: 'When' with 'else' branch in the first position.
fun case_1(value_1: Int): String = when (value_1) {
    <!ELSE_MISPLACED_IN_WHEN!>else<!> -> ""
    <!UNREACHABLE_CODE!>1 -> ""<!>
}

// CASE DESCRIPTION: 'When' with 'else' branch in the middle position.
fun case_2(value_1: Int): String = when (value_1) {
    1 -> ""
    <!ELSE_MISPLACED_IN_WHEN!>else<!> -> ""
    <!UNREACHABLE_CODE!>2 -> ""<!>
}

// CASE DESCRIPTION: 'When' with two 'else' branches.
fun case_3(value_1: Int): String {
    when (value_1) {
        <!ELSE_MISPLACED_IN_WHEN!>else<!> -> return ""
        <!UNREACHABLE_CODE!>else -> return ""<!>
    }

    <!UNREACHABLE_CODE!>return ""<!>
}
