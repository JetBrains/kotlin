/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 6
 SENTENCE 1: When expression with bound value (the form where the expression enclosed in parantheses is present) are very similar to the form without bound value, but use different syntax for conditions.
 NUMBER: 12
 DESCRIPTION: 'When' with prefix operator expression in the control structure body.
 */

fun case_1(value: Int, value1: Int, value2: Int, value3: Boolean) {
    var mutableValue1 = value1
    var mutableValue2 = value2

    when (value) {
        1 -> ++mutableValue1
        2 -> --mutableValue2
        3 -> --mutableValue1 - ++mutableValue2
        5 -> ++mutableValue1 + --mutableValue1
        6 -> !value3
        7 -> !!!!!!value3
        8 -> {
            ++mutableValue1 + --mutableValue1
        }
    }
}