/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-100
 * PLACE: expressions, when-expression -> paragraph 5 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: 'When' with bound value and empty 'when condition'.
 */

fun case_1() {
    when (value) {
        -> { println(1) }
    }
}