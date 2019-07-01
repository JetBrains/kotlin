/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: when-expression -> paragraph 6 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: 'When' with bound value and empty control structure body.
 */

fun case_1() {
    when (value) {
        1 ->
    }
}