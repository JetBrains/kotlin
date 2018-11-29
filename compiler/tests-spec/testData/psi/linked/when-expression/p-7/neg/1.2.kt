/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SECTIONS: when-expression
 * PARAGRAPH: 7
 * SENTENCE: [1] Type test condition: type checking operator followed by type.
 * NUMBER: 2
 * DESCRIPTION: 'When' with bound value and 'when condition' with type checking operator and non-type value.
 * UNEXPECTED BEHAVIOUR
 */

fun case_2() {
    when (value) {
        is value -> return ""
        is value -> return ""
        is value.isEmpty() -> return ""
    }
    when (value) {
        is {} -> return ""
        is fun() {} -> return ""
        is 90 -> return ""
        is -.032 -> return ""
        is "..." -> return ""
        is '.' -> return ""
        is return 1 -> return ""
        is throw Exception() -> return ""
    }
}
