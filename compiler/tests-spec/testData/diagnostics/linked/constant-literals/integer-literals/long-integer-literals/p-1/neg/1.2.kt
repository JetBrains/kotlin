/*
 KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)

 SECTIONS: constant-literals, integer-literals, long-integer-literals
 PARAGRAPH: 1
 SENTENCE: [1] Any of the decimal, hexadecimal or binary literals may be suffixed by the long literal mark (symbol L).
 NUMBER: 2
 DESCRIPTION: Various integer literals with a not allowed underscore before the long literal mark.
 */

val value_1 = <!ILLEGAL_UNDERSCORE!>0b0_<!WRONG_LONG_SUFFIX!>l<!><!>
val value_2 = <!ILLEGAL_UNDERSCORE, INT_LITERAL_OUT_OF_RANGE!>0B12_L<!>
val value_3 = <!ILLEGAL_UNDERSCORE!>0X234_<!WRONG_LONG_SUFFIX!>l<!><!>
val value_4 = <!ILLEGAL_UNDERSCORE!>0x3567_L<!>
