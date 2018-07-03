// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 3
 SENTENCE 1: When expression without bound value (the form where the expression enclosed in parantheses is absent) evaluates one of the many different expressions based on corresponding conditions present in the same when entry.
 NUMBER: 2
 DESCRIPTION: 'When' with different variants of the logical expressions in the control structure body.
 */

fun case_1(value: Int, value1: Boolean, value2: Boolean, value3: String, value4: Any?) {
    val value5 = true
    val value6 = false

    when {
        value == 1 -> !value1
        value == 2 -> value5 && value6 || value1 && !value3.isEmpty()
        value == 3 -> value1 || value2
        value == 4 -> true && value2
        value == 5 -> !!(!!(value4 == null))
        value == 6 -> value1 || !!!value5
        value == 7 -> value1 && !!true && value5 && value6
        value == 8 -> !!!!!!!!!value2
        value == 9 -> !!!!value6
        value == 10 -> {
            value6 && value1 || value5 || value6 && !!!false
        }
    }
}