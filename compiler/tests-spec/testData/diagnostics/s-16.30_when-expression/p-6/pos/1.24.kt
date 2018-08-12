/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 6
 SENTENCE 1: When expression with bound value (the form where the expression enclosed in parantheses is present) are very similar to the form without bound value, but use different syntax for conditions.
 NUMBER: 24
 DESCRIPTION: 'When' with break expression in the control structure body.
 */

fun case_1(value: Int) {
    loop1@ while (true) {
        loop2@ while (true) {
            when (value) {
                1 -> break@loop1
                2 -> {
                    break@loop2
                }
            }
        }
    }
}