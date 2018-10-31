/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SECTIONS: when-expression
 * PARAGRAPH: 2
 * SENTENCE: [3] When expression has two different forms: with bound value and without it.
 * NUMBER: 2
 * DESCRIPTION: Empty 'when' with missed 'when entries' section.
 */

fun case_1() {
    when (value)
    when ()
    when
}
