/*
 KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)

 SECTIONS: constant-literals, integer-literals, hexadecimal-integer-literals
 PARAGRAPH: 1
 SENTENCE: [1] A sequence of hexadecimal digit symbols (0 through 9, a through f, A through F) prefixed by 0x or 0X is a hexadecimal integer literal.
 NUMBER: 1
 DESCRIPTION: Hexadecimal integer literals with the prefix only.
 */

val value_1 = <!INT_LITERAL_OUT_OF_RANGE!>0x<!>
val value_2 = <!INT_LITERAL_OUT_OF_RANGE!>0X<!>
