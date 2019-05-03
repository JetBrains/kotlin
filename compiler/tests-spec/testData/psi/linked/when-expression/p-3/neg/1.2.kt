/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: when-expression -> paragraph 3 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: 'When' without bound value and missed 'when condition'.
 */

fun case_1() {
    when {
        -> { println(1) }
    }
}