/*
 KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)

 SECTIONS: constant-literals, integer-literals, decimal-integer-literals
 PARAGRAPH: 1
 SENTENCE: [2] Digits may be separated by an underscore symbol, but no underscore can be placed before the first digit or after the last one.
 NUMBER: 1
 DESCRIPTION: Integer literals with an underscore in the last position.
 */

val value_1 = <!ILLEGAL_UNDERSCORE!>1_<!>
val value_2 = <!ILLEGAL_UNDERSCORE!>1_00000000000000000_<!>
val value_3 = <!ILLEGAL_UNDERSCORE!>1_____________<!>
val value_4 = <!ILLEGAL_UNDERSCORE!>9____________0_<!>
val value_5 = <!ILLEGAL_UNDERSCORE!>1_______________________________________________________________________________________________________________________________________________________<!>
