/*
 KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)

 SECTIONS: constant-literals, integer-literals, long-integer-literals
 PARAGRAPH: 1
 SENTENCE: [1] Any of the decimal, hexadecimal or binary literals may be suffixed by the long literal mark (symbol L).
 NUMBER: 3
 DESCRIPTION: Various integer literals with not allowed long literal mark in lower case.
 */

val value_1 = 0<!WRONG_LONG_SUFFIX!>l<!>
val value_2 = 1000000000000000<!WRONG_LONG_SUFFIX!>l<!>
val value_3 = 0X0<!WRONG_LONG_SUFFIX!>l<!>
val value_4 = 0b101<!WRONG_LONG_SUFFIX!>l<!>
