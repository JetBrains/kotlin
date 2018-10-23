/*
 KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)

 SECTIONS: constant-literals, integer-literals, long-integer-literals
 PARAGRAPH: 1
 SENTENCE: [1] Any of the decimal, hexadecimal or binary literals may be suffixed by the long literal mark (symbol L).
 NUMBER: 1
 DESCRIPTION: Binary and hexadecimal integer literals with a long literal mark only.
 */

val value_1 = <!INT_LITERAL_OUT_OF_RANGE!>0bl<!>
val value_2 = <!INT_LITERAL_OUT_OF_RANGE!>0BL<!>
val value_3 = <!INT_LITERAL_OUT_OF_RANGE!>0Xl<!>
val value_4 = <!INT_LITERAL_OUT_OF_RANGE!>0xL<!>

val value_5 = <!ILLEGAL_UNDERSCORE, INT_LITERAL_OUT_OF_RANGE!>0b_l<!>
val value_6 = <!ILLEGAL_UNDERSCORE, INT_LITERAL_OUT_OF_RANGE!>0B_L<!>
val value_7 = <!ILLEGAL_UNDERSCORE, INT_LITERAL_OUT_OF_RANGE!>0X____l<!>
val value_8 = <!ILLEGAL_UNDERSCORE, INT_LITERAL_OUT_OF_RANGE!>0x_L<!>
