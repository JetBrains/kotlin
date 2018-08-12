// !DIAGNOSTICS: -UNUSED_EXPRESSION

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 6
 SENTENCE 1: When expression with bound value (the form where the expression enclosed in parantheses is present) are very similar to the form without bound value, but use different syntax for conditions.
 NUMBER: 2
 DESCRIPTION: 'When' with different variants of the logical expressions in the control structure body.
 */

fun case_1(value: Int, value1: Boolean, value2: Boolean) {
    val value3 = true
    val value4 = false

    when (value) {
        1 -> !value1
        2 -> value3 && value4 || value1
        3 -> value1 || value2
        4 -> value1 && value2
        5 -> !!value2
        6 -> value1 || !!!value3
        7 -> value1 && value2 && value3 && value4
        8 -> !!!!!!!!!value2
        9 -> !!!!value4
        10 -> {
            value4 && value1 || value3 || value4 && value1
        }
    }
}