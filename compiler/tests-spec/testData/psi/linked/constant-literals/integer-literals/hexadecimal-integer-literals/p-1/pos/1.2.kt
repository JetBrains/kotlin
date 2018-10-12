/*
 KOTLIN PSI SPEC TEST (POSITIVE)

 SECTIONS: constant-literals, integer-literals, hexadecimal-integer-literals
 PARAGRAPH: 1
 SENTENCE: [1] A sequence of hexadecimal digit symbols (0 through 9, a through f, A through F) prefixed by 0x or 0X is a hexadecimal integer literal.
 NUMBER: 2
 DESCRIPTION: Hexadecimal integer literals with prefix only (without digit symbols).
 */

val value = 0x
val value = 0X
