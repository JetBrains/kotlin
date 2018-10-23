/*
 KOTLIN PSI SPEC TEST (POSITIVE)

 SECTIONS: constant-literals, integer-literals, binary-integer-literals
 PARAGRAPH: 1
 SENTENCE: [2] Digits may be separated by an underscore symbol, but no underscore can be placed before the first digit or after the last one.
 NUMBER: 1
 DESCRIPTION: Binary integer literals with underscore symbols in the valid positions.
 */

val value = 0b0_00_01_11_11_00_00
val value = 0B0_1_0_1_0_1_0_1_0_1_0_1
val value = 0b101_010_10101
val value = 0B00000000000________________________0
val value = 0b0_0
val value = 0B00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000_00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
val value = 0B11111111111111111111111111_________1111111111111111111111111111111111111
val value = 0b1_______________________________________________________________________________________________________________________________________________________0
