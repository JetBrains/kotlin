/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 6
 SENTENCE 1: When expression with bound value (the form where the expression enclosed in parantheses is present) are very similar to the form without bound value, but use different syntax for conditions.
 NUMBER: 14
 DESCRIPTION: 'When' with indexing expression in the control structure body.
 */

fun case_1(value: Int, value1: List<Int>, value2: List<List<List<List<Int>>>>?) {
    when (value) {
        1 -> value1[0]
        2 -> value2!![0][1]
        3 -> value2!![0][1][-1]
        4 -> {
            value2!![0][0][0][0]
        }
    }
}