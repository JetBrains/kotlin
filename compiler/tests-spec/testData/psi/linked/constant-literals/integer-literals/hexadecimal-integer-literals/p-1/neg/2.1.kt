/*
 KOTLIN PSI SPEC TEST (NEGATIVE)

 SECTIONS: constant-literals, integer-literals, hexadecimal-integer-literals
 PARAGRAPH: 1
 SENTENCE: [2] Digits may be separated by an underscore symbol, but no underscore can be placed before the first digit or after the last one.
 NUMBER: 1
 DESCRIPTION: Hexadecimal integer literals with underscore breaking the prefix (in it).
 */

val value = 0_x3_4_5_6_7_8
val value = 0_X_______4_______5_______6_______7
val value = 0_0X4_3_4_5_6_7_8_9
val value = 0_0X000000000
val value = 0_0000000000X
val value = 0_9x
val value = 0____________0x
val value = 0_0_x_0
val value = 0_x_0
val value = 0_x
val value = 0_x_
val value = 0_x_0_
