/*
 KOTLIN PSI SPEC TEST (POSITIVE)

 SECTIONS: constant-literals, integer-literals, binary-integer-literals
 PARAGRAPH: 1
 SENTENCE: [2] Digits may be separated by an underscore symbol, but no underscore can be placed before the first digit or after the last one.
 NUMBER: 2
 DESCRIPTION: Binary integer literals with underscore symbols after binary prefix.
 */

val value = 0b_000111010001111
val value = 0b_______011001
val value = 0B_0_1_0_1_1_0_0
val value = 0b_
