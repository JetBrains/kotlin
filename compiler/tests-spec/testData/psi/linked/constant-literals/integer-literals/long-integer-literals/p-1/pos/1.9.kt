/*
 * KOTLIN PSI SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, integer-literals, long-integer-literals -> paragraph 1 -> sentence 1
 * NUMBER: 9
 * DESCRIPTION: Various integer literals with long literal mark in not allowed lower case (but valid in opinion the parser).
 */

val value = 1234567890l
val value = 1l
val value = 1_l
val value = 1234_5678_90l
val value = 0x0123456789abcdefl
val value = 0x1l
val value = 0Xal
val value = 0xA0Al
val value = 0xl
val value = 0X4_______5_______d_______7l
val value = 0x_l
val value = 0b10101010101l
val value = 0b000000009l
val value = 0bl
val value = 0B1_______0_______1_______0_l
val value = 0B______________l
