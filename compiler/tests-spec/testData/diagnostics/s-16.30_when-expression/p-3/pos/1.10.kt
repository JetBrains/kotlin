// SKIP_TXT

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 3
 SENTENCE 1: When expression without bound value (the form where the expression enclosed in parantheses is absent) evaluates one of the many different expressions based on corresponding conditions present in the same when entry.
 NUMBER: 10
 DESCRIPTION: 'When' with range expression in the control structure body.
 */

fun case_1(value: Int, value1: Int?, value2: Int) {
    when {
        value == 1 -> 1..10
        value == 2 -> 1..1
        value == 3 -> 1..-10
        value == 4 -> value1!!..4
        value == 5 -> 1..value2
        value == 6 -> {
            11..192391293912931L
        }
    }
}