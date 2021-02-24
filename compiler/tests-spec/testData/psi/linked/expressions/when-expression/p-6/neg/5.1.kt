/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-435
 * MAIN LINK: expressions, when-expression -> paragraph 6 -> sentence 5
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
