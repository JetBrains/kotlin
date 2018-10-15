/*
 KOTLIN PSI SPEC TEST (POSITIVE)

 SECTIONS: constant-literals, integer-literals, binary-integer-literals
 PARAGRAPH: 1
 SENTENCE: [2] Digits may be separated by an underscore symbol, but no underscore can be placed before the first digit or after the last one.
 NUMBER: 3
 DESCRIPTION: Binary integer literals with underscore symbol after the last digit.
 */

val value = 0b0_1_0_1_0_1_____
val value = 0B1_______0_______1_______0_
val value = 0B000000000_
val value = 0b_
val value = 0B______________
val value = 0B0_
val value = 0b10_
