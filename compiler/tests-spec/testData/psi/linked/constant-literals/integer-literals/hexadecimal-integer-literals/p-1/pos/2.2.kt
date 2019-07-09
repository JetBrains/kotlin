/*
 * KOTLIN PSI SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, integer-literals, hexadecimal-integer-literals -> paragraph 1 -> sentence 2
 * NUMBER: 2
 * DESCRIPTION: Hexadecimal integer literals with underscore symbols after hexadecimal prefix.
 */

val value = 0x_a2b45f789e
val value = 0X_______2f45c7d9
val value = 0X_a_3_4_5_6_7_e_e
val value = 0x_
