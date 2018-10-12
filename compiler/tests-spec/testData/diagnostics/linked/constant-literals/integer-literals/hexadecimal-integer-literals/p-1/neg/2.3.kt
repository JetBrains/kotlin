/*
 KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)

 SECTIONS: constant-literals, integer-literals, hexadecimal-integer-literals
 PARAGRAPH: 1
 SENTENCE: [2] A sequence of hexadecimal digit symbols (0 through 9, a through f, A through F) prefixed by 0x or 0X is a hexadecimal integer literal.
 NUMBER: 3
 DESCRIPTION: Hexadecimal integer literals with an underscore in the last position.
 */

val value_1 = <!ILLEGAL_UNDERSCORE!>0x3_4_5_6_7_8_____<!>
val value_2 = <!ILLEGAL_UNDERSCORE!>0X4_______5_______6_______7_<!>
val value_3 = <!ILLEGAL_UNDERSCORE!>0X000000000_<!>
val value_5 = <!ILLEGAL_UNDERSCORE, INT_LITERAL_OUT_OF_RANGE!>0x_<!>
val value_6 = <!ILLEGAL_UNDERSCORE, INT_LITERAL_OUT_OF_RANGE!>0X______________<!>
val value_7 = <!ILLEGAL_UNDERSCORE!>0X0_<!>
val value_8 = <!ILLEGAL_UNDERSCORE!>0X10_<!>
