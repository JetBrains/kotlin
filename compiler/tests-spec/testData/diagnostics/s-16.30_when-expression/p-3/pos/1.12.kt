/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 3
 SENTENCE 1: When expression without bound value (the form where the expression enclosed in parantheses is absent) evaluates one of the many different expressions based on corresponding conditions present in the same when entry.
 NUMBER: 12
 DESCRIPTION: 'When' with prefix operator expression in the control structure body.
 */

fun case_1(value: Int, value1: Int, value2: Int, value3: Boolean) {
    var mutableValue1 = value1
    var mutableValue2 = value2

    when {
        value == 1 -> ++mutableValue1
        value == 2 -> --mutableValue2
        value == 3 -> --mutableValue1 - ++mutableValue2
        value == 5 -> ++mutableValue1 + --mutableValue1
        value == 5 -> !value3
        value == 6 -> !!!!!!value3
        value == 7 -> {
            ++mutableValue1 + --mutableValue1
        }
    }
}