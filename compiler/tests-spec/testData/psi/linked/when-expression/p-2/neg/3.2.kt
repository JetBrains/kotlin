/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: when-expression -> paragraph 2 -> sentence 3
 * NUMBER: 2
 * DESCRIPTION: Empty 'when' with missed 'when entries' section.
 */

fun case_1() {
    when (value)
    when ()
    when
}
