/*
 KOTLIN PSI SPEC TEST (POSITIVE)

 SECTIONS: constant-literals, integer-literals, decimal-integer-literals
 PARAGRAPH: 1
 SENTENCE: [2] Digits may be separated by an underscore symbol, but no underscore can be placed before the first digit or after the last one.
 NUMBER: 1
 DESCRIPTION: Integers with underscore symbols in the valid positions.
 */

val value = 1234_5678_90
val value = 1_2_3_4_5_6_7_8_9_0
val value = 1_2
val value = 1_00000000000000000_1
val value = 1_____________2
val value = 9_____________0000
val value = 9____________0_0000
val value = 1_______________________________________________________________________________________________________________________________________________________0
