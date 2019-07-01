/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, integer-literals, binary-integer-literals -> paragraph 1 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: Binary integer literals with underscore breaking the prefix (in it).
 */

val value = 0_b1_1_0_1_0_1
val value = 0_B_______1_______0_______1_______0
val value = 0_0B1_1_1_0_1_0_1_0
val value = 0_0B000000000
val value = 0_0000000000B
val value = 0_0b
val value = 0____________0b
val value = 0_0_b_0
val value = 0_b_0
val value = 0_b
val value = 0_b_
val value = 0_b_0_
