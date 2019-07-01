/*
 * KOTLIN PSI SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, integer-literals, binary-integer-literals -> paragraph 1 -> sentence 2
 * NUMBER: 2
 * DESCRIPTION: Binary integer literals with underscore symbols after binary prefix.
 */

val value = 0b_000111010001111
val value = 0b_______011001
val value = 0B_0_1_0_1_1_0_0
val value = 0b_
