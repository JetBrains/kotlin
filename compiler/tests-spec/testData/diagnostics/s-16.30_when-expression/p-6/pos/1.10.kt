/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 6
 SENTENCE 1: When expression with bound value (the form where the expression enclosed in parantheses is present) are very similar to the form without bound value, but use different syntax for conditions.
 NUMBER: 10
 DESCRIPTION: 'When' with range expression in the control structure body.
 */

fun case_1(value: Int, value1: Int?, value2: Int) {
    when (value) {
        1 -> 1..10
        2 -> 1..1
        3 -> 1..-10
        4 -> value1!!..4
        5 -> 1..value2
        6 -> {
            11..192391293912931L
        }
    }
}