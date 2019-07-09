/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: when-expression -> paragraph 7 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: 'When' with bound value and 'when condition' with type checking operator and non-type value.
 * UNEXPECTED BEHAVIOUR
 * EXCEPTION: compiler
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
