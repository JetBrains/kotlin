/*
 KOTLIN PSI SPEC TEST (POSITIVE)

 SECTIONS: constant-literals, integer-literals, hexadecimal-integer-literals
 PARAGRAPH: 1
 SENTENCE: [2] Digits may be separated by an underscore symbol, but no underscore can be placed before the first digit or after the last one.
 NUMBER: 4
 DESCRIPTION: Hexadecimal integer literals with underscore symbol before the first digit (it's considered as identifiers).
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
