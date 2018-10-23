/*
 KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)

 SECTIONS: constant-literals, integer-literals, decimal-integer-literals
 PARAGRAPH: 1
 SENTENCE: [2] Digits may be separated by an underscore symbol, but no underscore can be placed before the first digit or after the last one.
 NUMBER: 2
 DESCRIPTION: Integer literals with an underscore in the first position (it's considered as identifiers).
 */

val value_1 = <!UNRESOLVED_REFERENCE!>_5678_90<!>
val value_2 = <!UNRESOLVED_REFERENCE!>_2_3_4_5_6_7_8_9_<!>
val value_3 = <!UNRESOLVED_REFERENCE!>_____________0000<!>
val value_4 = <!UNRESOLVED_REFERENCE!>_______________________________________________________________________________________________________________________________________________________0<!>
val value_5 = <!UNRESOLVED_REFERENCE!>____________________________________________________<!>
val value_6 = <!UNRESOLVED_REFERENCE!>_<!>
val value_7 = <!UNRESOLVED_REFERENCE!>_0_<!>
val value_8 = <!UNRESOLVED_REFERENCE!>_9_<!>
