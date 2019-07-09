/*
 * KOTLIN PSI SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, integer-literals, binary-integer-literals -> paragraph 1 -> sentence 2
 * NUMBER: 3
 * DESCRIPTION: Binary integer literals with underscore symbol after the last digit.
 */

val value = 0b0_1_0_1_0_1_____
val value = 0B1_______0_______1_______0_
val value = 0B000000000_
val value = 0b_
val value = 0B______________
val value = 0B0_
val value = 0b10_
