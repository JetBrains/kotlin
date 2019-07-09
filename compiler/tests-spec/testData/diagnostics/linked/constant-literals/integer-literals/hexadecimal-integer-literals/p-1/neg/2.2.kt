/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, integer-literals, hexadecimal-integer-literals -> paragraph 1 -> sentence 2
 * NUMBER: 2
 * DESCRIPTION: Hexadecimal integer literals with an underscore in the first position (it's considered as identifiers).
 */

// TESTCASE NUMBER: 1
val value_1 = <!UNRESOLVED_REFERENCE!>_____0x3_4_5_6_7_8<!>

// TESTCASE NUMBER: 2
val value_2 = <!UNRESOLVED_REFERENCE!>_0X4_______5_______6_______7<!>

// TESTCASE NUMBER: 3
val value_3 = <!UNRESOLVED_REFERENCE!>_0_0X4_3_4_5_6_7_8_9<!>

// TESTCASE NUMBER: 4
val value_4 = <!UNRESOLVED_REFERENCE!>_0X000000000<!>

// TESTCASE NUMBER: 5
val value_5 = <!UNRESOLVED_REFERENCE!>_0000000000x<!>

// TESTCASE NUMBER: 6
val value_6 = <!UNRESOLVED_REFERENCE!>_0_9x<!>

// TESTCASE NUMBER: 7
val value_7 = <!UNRESOLVED_REFERENCE!>____________0x<!>

// TESTCASE NUMBER: 8
val value_8 = <!UNRESOLVED_REFERENCE!>_0_x_0<!>

// TESTCASE NUMBER: 9
val value_9 = <!UNRESOLVED_REFERENCE!>_x_0<!>

// TESTCASE NUMBER: 10
val value_10 = <!UNRESOLVED_REFERENCE!>_x<!>

// TESTCASE NUMBER: 11
val value_11 = <!UNRESOLVED_REFERENCE!>_x_<!>
