/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-100
 * PLACE: expressions, when-expression -> paragraph 6 -> sentence 8
 * NUMBER: 1
 * DESCRIPTION: 'When' with bound value and not allowed spread operator in 'when condition'.
 */

fun case_1() {
    when (value) {
        *value -> return ""
        *arrayOf("a", "b", "c") -> return ""
        *listOf(null, null, null) -> return ""
    }
}
