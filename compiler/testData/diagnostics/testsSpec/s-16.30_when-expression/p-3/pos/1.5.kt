/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 3
 SENTENCE 1: When expression without bound value (the form where the expression enclosed in parantheses is absent) evaluates one of the many different expressions based on corresponding conditions present in the same when entry.
 NUMBER: 5
 DESCRIPTION: 'When' with concatenations in the control structure body.
 */

fun case_1(value: Int, value1: String, value2: String) {
    when {
        value == 1 -> value1 + value2
        value == 2 -> value1 + ""
        value == 3 -> "1" + "" + "..."
        value == 4 -> "..." + value1 + "" + "$value2" + "a"
        value == 5 -> "..." + value1 + "$value2"
        value == 6 -> value1 + "" + value2 + "a"
        value == 7 -> value1 + value2 + "a"
        value == 8 -> {
            "..." + value1 + "" + value2 + "a" + "$value2 " + " ...$value2$value1" + "${value1}"
        }
    }
}