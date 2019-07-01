/*
 * KOTLIN PSI SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, integer-literals, long-integer-literals -> paragraph 1 -> sentence 1
 * NUMBER: 5
 * DESCRIPTION: Hexadecimal integer literals with underscores suffixed by the long literal mark.
 */

val value = 0x1_234C67890L
val value = 0XF_______3456789L
val value = 0x3_4_5_6_7_8L
val value = 0X4_______5_______d_______7L
val value = 0X5__________________________________________________________________________________________________6L
val value = 0x0_______BL
val value = 0X0_0L
val value = 0xB_______________________________________________________________________________________________________________________________________________________0L
val value = 0x1_00000000000000000_1L

val value = 0x_a2b45f789eL
val value = 0X_______2f45c7d9L
val value = 0X_a_3_4_5_6_7_e_eL
val value = 0x_L

val value = 0x3_c_c_c_7_8_____L
val value = 0Xc_______5_______6_______F_L
val value = 0X000000000_L
val value = 0x_L
val value = 0X______________L
val value = 0X0_L
val value = 0X1e_L
