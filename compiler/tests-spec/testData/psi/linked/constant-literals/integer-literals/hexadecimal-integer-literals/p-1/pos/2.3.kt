/*
 * KOTLIN PSI SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, integer-literals, hexadecimal-integer-literals -> paragraph 1 -> sentence 2
 * NUMBER: 3
 * DESCRIPTION: Hexadecimal integer literals with underscore symbol after the last digit.
 */

val value = 0x3_c_c_c_7_8_____
val value = 0Xc_______5_______6_______F_
val value = 0X000000000_
val value = 0x_
val value = 0X______________
val value = 0X0_
val value = 0X1e_
