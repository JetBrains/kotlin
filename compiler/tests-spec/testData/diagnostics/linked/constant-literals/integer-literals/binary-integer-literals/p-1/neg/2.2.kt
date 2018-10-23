/*
 KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)

 SECTIONS: constant-literals, integer-literals, binary-integer-literals
 PARAGRAPH: 1
 SENTENCE: [2] Digits may be separated by an underscore symbol, but no underscore can be placed before the first digit or after the last one.
 NUMBER: 2
 DESCRIPTION: Binary integer literals with an underscore in the first position (it's considered as identifiers).
 */

val value_0 = <!UNRESOLVED_REFERENCE!>_____0b0_1_1_1_0_1<!>
val value_1 = <!UNRESOLVED_REFERENCE!>_0B1_______1_______1_______0<!>
val value_2 = <!UNRESOLVED_REFERENCE!>_0_0B1_0_1_1_1_0_1_1<!>
val value_3 = <!UNRESOLVED_REFERENCE!>_0B000000000<!>
val value_4 = <!UNRESOLVED_REFERENCE!>_0000000000b<!>
val value_5 = <!UNRESOLVED_REFERENCE!>_0_1b<!>
val value_6 = <!UNRESOLVED_REFERENCE!>____________0b<!>
val value_7 = <!UNRESOLVED_REFERENCE!>_0_b_0<!>
val value_8 = <!UNRESOLVED_REFERENCE!>_b_0<!>
val value_9 = <!UNRESOLVED_REFERENCE!>_b<!>
val value_10 = <!UNRESOLVED_REFERENCE!>_b_<!>
