// !DIAGNOSTICS: -UNUSED_EXPRESSION

/*
 KOTLIN SPEC TEST (NEGATIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 7
 SENTENCE 7: The else condition, which works the exact same way as it would in the form without bound expression.
 NUMBER: 2
 DESCRIPTION: 'When' with bound value and with else branch not in the last position.
 */

fun case_1(value: Int): String = when (value) {
    <!ELSE_MISPLACED_IN_WHEN!>else<!> -> ""
    <!UNREACHABLE_CODE!>1 -> ""<!>
}

fun case_2(value: Int): String = when (value) {
    <!ELSE_MISPLACED_IN_WHEN!>else<!> -> ""
    <!UNREACHABLE_CODE!>1 -> ""<!>
    <!UNREACHABLE_CODE!>2 -> ""<!>
}

fun case_3(value: Int): String = when (value) {
    1 -> ""
    <!ELSE_MISPLACED_IN_WHEN!>else<!> -> ""
    <!UNREACHABLE_CODE!>2 -> ""<!>
}

fun case_4(value: Int): String {
    when (value) {
        <!ELSE_MISPLACED_IN_WHEN!>else<!> -> return ""
        <!UNREACHABLE_CODE!>else -> return ""<!>
    }

    <!UNREACHABLE_CODE!>return ""<!>
}
