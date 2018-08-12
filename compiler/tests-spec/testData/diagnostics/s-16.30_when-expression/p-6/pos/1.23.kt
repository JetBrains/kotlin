/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 6
 SENTENCE 1: When expression with bound value (the form where the expression enclosed in parantheses is present) are very similar to the form without bound value, but use different syntax for conditions.
 NUMBER: 23
 DESCRIPTION: 'When' with continue expression in the control structure body.
 */

fun case_1(value: Int) {
    loop1@ while (true) {
        loop2@ while (true) {
            when (value) {
                1 -> continue@loop1
                2 -> {
                    continue@loop2
                }
            }
        }
    }
}