/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: when-expression -> paragraph 7 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: 'When' with bound value and type test condition, but missed type in 'when condition'.
 */

fun case_1() {
    when (value) {
        is -> return ""
    }
    when (value) {
        is -> return ""
        is -> return ""
    }
}
