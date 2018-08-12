/*
 KOTLIN SPEC TEST (NEGATIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 5
 SENTENCE 1: The else entry is also special in the sense that it must be the last entry in the expression, otherwise a compiler error must be generated.
 NUMBER: 1
 DESCRIPTION: 'When' without bound value and with 'else' branch not in the last position.
 */

// CASE DESCRIPTION: 'When' with 'else' branch followed by one not 'else' branch.
fun case_1(<!UNUSED_PARAMETER!>value<!>: Int): String = when {
    <!ELSE_MISPLACED_IN_WHEN!>else<!> -> ""
    <!UNREACHABLE_CODE!>value == 1 -> ""<!>
}

// CASE DESCRIPTION: 'When' with 'else' branch followed by two not 'else' branches.
fun case_2(<!UNUSED_PARAMETER!>value<!>: Int): String = when {
    <!ELSE_MISPLACED_IN_WHEN!>else<!> -> ""
    <!UNREACHABLE_CODE!>value == 1 -> ""<!>
    <!UNREACHABLE_CODE!>value == 2 -> ""<!>
}

// CASE DESCRIPTION: 'When' with not 'else' branches followed by 'else' branch followed by not 'else' branch.
fun case_3(value: Int): String = when {
    value == 1 -> ""
    <!ELSE_MISPLACED_IN_WHEN!>else<!> -> ""
    <!UNREACHABLE_CODE!>value == 2 -> ""<!>
}

// CASE DESCRIPTION: 'When' with two 'else' branches.
fun case_4(): String {
    when {
        <!ELSE_MISPLACED_IN_WHEN!>else<!> -> return ""
        <!UNREACHABLE_CODE!>else -> return ""<!>
    }

    <!UNREACHABLE_CODE!>return ""<!>
}
