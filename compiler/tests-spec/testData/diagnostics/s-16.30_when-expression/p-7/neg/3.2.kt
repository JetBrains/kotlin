// !DIAGNOSTICS: -UNUSED_EXPRESSION

/*
 KOTLIN SPEC TEST (NEGATIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 7
 SENTENCE 3: Contains test condition: containment operator followed by an expression.
 NUMBER: 2
 DESCRIPTION: 'When' with bound value and 'when condition' with range expression, but without contains operator.
 */

// CASE DESCRIPTION: 'When' with one contains operator.
fun case_1(value: Int): String {
    when (value) {
        in<!SYNTAX!><!> -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with two contains operators.
fun case_2(value: Int): String {
    when (value) {
        in<!SYNTAX!><!> -> return ""
        in<!SYNTAX!><!> -> return ""
    }

    return ""
}
