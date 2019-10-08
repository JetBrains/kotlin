/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-100
 * PLACE: expressions, when-expression -> paragraph 1 -> sentence 3
 * NUMBER: 2
 * DESCRIPTION: Empty 'when' with missed 'when entries' section.
 */

fun case_1() {
    when (value)
    when ()
    when
}
