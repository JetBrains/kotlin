/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-313
 * MAIN LINK: expressions, when-expression -> paragraph 5 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: 'When' with bound value and empty control structure body.
 */

fun case_1() {
    when (value) {
        1 ->
    }
}