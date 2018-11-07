/*
 KOTLIN PSI SPEC TEST (POSITIVE)

 SECTIONS: constant-literals, integer-literals, binary-integer-literals
 PARAGRAPH: 1
 SENTENCE: [1] A sequence of binary digit symbols (0 or 1) prefixed by 0b or 0B is a binary integer literal.
 NUMBER: 2
 DESCRIPTION: Binary integer literals with invalid [2-9] digit symbols.
 */

val value = 0B000123412
val value = 0b000000009
val value = 0b200000000
val value = 0b1234567890
