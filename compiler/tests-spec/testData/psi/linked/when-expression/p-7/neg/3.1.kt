/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: when-expression -> paragraph 7 -> sentence 3
 * NUMBER: 1
 * DESCRIPTION: 'When' with bound value and 'when condition' with range expression, but without contains operator.
 */

fun case_1() {
    when (value) {
        in -> return ""
    }
    when (value) {
        in -> return ""
        in -> return ""
    }
}
