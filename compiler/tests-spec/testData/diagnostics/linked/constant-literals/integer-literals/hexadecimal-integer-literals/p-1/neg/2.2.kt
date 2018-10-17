/*
 KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)

 SECTIONS: constant-literals, integer-literals, hexadecimal-integer-literals
 PARAGRAPH: 1
 SENTENCE: [2] Digits may be separated by an underscore symbol, but no underscore can be placed before the first digit or after the last one.
 NUMBER: 2
 DESCRIPTION: Hexadecimal integer literals with an underscore in the first position (it's considered as identifiers).
 */

val value_1 = <!UNRESOLVED_REFERENCE!>_____0x3_4_5_6_7_8<!>
val value_2 = <!UNRESOLVED_REFERENCE!>_0X4_______5_______6_______7<!>
val value_3 = <!UNRESOLVED_REFERENCE!>_0_0X4_3_4_5_6_7_8_9<!>
val value_4 = <!UNRESOLVED_REFERENCE!>_0X000000000<!>
val value_5 = <!UNRESOLVED_REFERENCE!>_0000000000x<!>
val value_6 = <!UNRESOLVED_REFERENCE!>_0_9x<!>
val value_7 = <!UNRESOLVED_REFERENCE!>____________0x<!>
val value_8 = <!UNRESOLVED_REFERENCE!>_0_x_0<!>
val value_9 = <!UNRESOLVED_REFERENCE!>_x_0<!>
val value_10 = <!UNRESOLVED_REFERENCE!>_x<!>
val value_11 = <!UNRESOLVED_REFERENCE!>_x_<!>
