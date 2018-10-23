/*
 KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)

 SECTIONS: constant-literals, integer-literals, binary-integer-literals
 PARAGRAPH: 1
 SENTENCE: [2] Digits may be separated by an underscore symbol, but no underscore can be placed before the first digit or after the last one.
 NUMBER: 3
 DESCRIPTION: Binary integer literals with an underscore in the last position.
 */

val value_1 = <!ILLEGAL_UNDERSCORE!>0b0_1_1_0_1_1_____<!>
val value_2 = <!ILLEGAL_UNDERSCORE!>0B1_______1_______0_______1_<!>
val value_3 = <!ILLEGAL_UNDERSCORE!>0B000000000_<!>
val value_4 = <!ILLEGAL_UNDERSCORE, INT_LITERAL_OUT_OF_RANGE!>0b_<!>
val value_5 = <!ILLEGAL_UNDERSCORE, INT_LITERAL_OUT_OF_RANGE!>0B______________<!>
val value_6 = <!ILLEGAL_UNDERSCORE!>0B0_<!>
val value_7 = <!ILLEGAL_UNDERSCORE!>0B10_<!>
