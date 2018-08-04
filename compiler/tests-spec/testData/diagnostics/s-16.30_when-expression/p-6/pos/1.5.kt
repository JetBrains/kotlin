/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 6
 SENTENCE 1: When expression with bound value (the form where the expression enclosed in parantheses is present) are very similar to the form without bound value, but use different syntax for conditions.
 NUMBER: 5
 DESCRIPTION: 'When' with concatenations in the control structure body.
 */

fun case_1(value: Int, value1: String, value2: String) {
    when (value) {
        1 -> value1 + value2
        2 -> value1 + ""
        3 -> "1" + "" + "..."
        4 -> "..." + value1 + "" + "$value2" + "a"
        5 -> "..." + value1 + "$value2"
        6 -> value1 + "" + value2 + "a"
        7 -> value1 + value2 + "a"
        8 -> {
            "..." + value1 + "" + value2 + "a" + "$value2 " + " ...$value2$value1" + "${value1}"
        }
    }
}