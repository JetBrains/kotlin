/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 3
 SENTENCE 1: When expression without bound value (the form where the expression enclosed in parantheses is absent) evaluates one of the many different expressions based on corresponding conditions present in the same when entry.
 NUMBER: 14
 DESCRIPTION: 'When' with indexing expression in the control structure body.
 */

fun case_1(value: Int, value1: List<Int>, value2: List<List<List<List<Int>>>>?) {
    when {
        value == 1 -> value1[0]
        value == 2 -> value2!![0][1]
        value == 3 -> value2!![0][1][-1]
        value == 4 -> {
            value2!![0][0][0][0]
        }
    }
}