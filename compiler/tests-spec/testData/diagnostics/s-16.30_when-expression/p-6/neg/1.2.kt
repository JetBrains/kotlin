/*
 KOTLIN SPEC TEST (NEGATIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 6
 SENTENCE 1: When expression with bound value (the form where the expression enclosed in parantheses is present) are very similar to the form without bound value, but use different syntax for conditions.
 NUMBER: 2
 DESCRIPTION: 'When' with bound value and empty 'when condition'.
 */

fun case_1(value: Int) {
    when (value) {
        <!SYNTAX!><!>-> { println(1) }
    }
}