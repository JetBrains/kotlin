/*
 KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)

 SECTIONS: constant-literals, integer-literals, hexadecimal-integer-literals
 PARAGRAPH: 1
 SENTENCE: [2] Digits may be separated by an underscore symbol, but no underscore can be placed before the first digit or after the last one.
 NUMBER: 1
 DESCRIPTION: Hexadecimal integer literals with an underscore after the prefix.
 */

val value_1 = <!ILLEGAL_UNDERSCORE!>0x_1234567890<!>
val value_2 = <!ILLEGAL_UNDERSCORE!>0X_______23456789<!>
val value_3 = <!ILLEGAL_UNDERSCORE!>0X_2_3_4_5_6_7_8_9<!>
val value_4 = <!ILLEGAL_UNDERSCORE, INT_LITERAL_OUT_OF_RANGE!>0x_<!>
