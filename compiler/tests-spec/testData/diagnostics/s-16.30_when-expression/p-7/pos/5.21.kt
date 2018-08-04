/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 7
 SENTENCE 5: Any other expression.
 NUMBER: 21
 DESCRIPTION: 'When' with bound value and throw expression in 'when condition'.
 */

// CASE DESCRIPTION: 'When' with 'else' branch (as expression) and only one throw expression.
fun case_1(value: Any?): String = when (value) {
    throw Exception("Ex")<!UNREACHABLE_CODE!><!> -> <!UNREACHABLE_CODE!>""<!>
    <!UNREACHABLE_CODE!>else -> ""<!>
}

// CASE DESCRIPTION: 'When' without 'else' branch (as statement) and only one throw expression.
fun case_2(value: Any?): String {
    when (value) {
        throw Exception("Ex")<!UNREACHABLE_CODE!><!> -> <!UNREACHABLE_CODE!>return ""<!>
    }

    <!UNREACHABLE_CODE!>return ""<!>
}

// CASE DESCRIPTION: 'When' with 'else' branch (as expression) and several throw expressions.
fun case_3(value: Any?): String = when (value) {
    throw Exception("Ex")<!UNREACHABLE_CODE!><!> -> <!UNREACHABLE_CODE!>""<!>
    <!UNREACHABLE_CODE!>throw Exception("Ex") -> ""<!>
    <!UNREACHABLE_CODE!>else -> ""<!>
}

// CASE DESCRIPTION: 'When' without 'else' branch (as statement) and several throw expressions.
fun case_4(value: Any?): String {
    when (value) {
        throw Exception("Ex")<!UNREACHABLE_CODE!><!> -> <!UNREACHABLE_CODE!>return ""<!>
        <!UNREACHABLE_CODE!>throw Exception("Ex") -> return ""<!>
    }

    <!UNREACHABLE_CODE!>return ""<!>
}
