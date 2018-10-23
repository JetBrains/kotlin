/*
 KOTLIN PSI SPEC TEST (POSITIVE)

 SECTIONS: constant-literals, integer-literals, hexadecimal-integer-literals
 PARAGRAPH: 1
 SENTENCE: [2] Digits may be separated by an underscore symbol, but no underscore can be placed before the first digit or after the last one.
 NUMBER: 2
 DESCRIPTION: Hexadecimal integer literals with underscore symbols after hexadecimal prefix.
 */

val value = 0x_a2b45f789e
val value = 0X_______2f45c7d9
val value = 0X_a_3_4_5_6_7_e_e
val value = 0x_
