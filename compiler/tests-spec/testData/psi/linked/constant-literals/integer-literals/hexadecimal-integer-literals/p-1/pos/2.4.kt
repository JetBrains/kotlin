/*
 * KOTLIN PSI SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, integer-literals, hexadecimal-integer-literals -> paragraph 1 -> sentence 2
 * NUMBER: 4
 * DESCRIPTION: Hexadecimal integer literals with underscore symbol before the first digit (it's considered as identifiers).
 */

val value = _____0x3_4_5_6_7_8
val value = _0X4_______5_______6_______7
val value = _0_0X4_3_4_5_6_7_8_9
val value = _0X000000000
val value = _0000000000x
val value = _0_9x
val value = ____________0x
val value = _0_x_0
val value = _x_0
val value = _x
val value = _x_
