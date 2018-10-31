/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SECTIONS: when-expression
 * PARAGRAPH: 7
 * SENTENCE: [5] Any other expression.
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
