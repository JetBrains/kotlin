/*
 * KOTLIN PSI SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, integer-literals, hexadecimal-integer-literals -> paragraph 1 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: Hexadecimal integer literals with underscore symbols in valid places.
 */

val value = 0x1_234C67890
val value = 0XF_______3456789
val value = 0x3_4_5_6_7_8
val value = 0X4_______5_______d_______7
val value = 0X5__________________________________________________________________________________________________6
val value = 0x0_______B
val value = 0X0_0
val value = 0xB_______________________________________________________________________________________________________________________________________________________0
val value = 0x1_00000000000000000_1
