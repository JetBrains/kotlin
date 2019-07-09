/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, integer-literals, binary-integer-literals -> paragraph 1 -> sentence 2
 * NUMBER: 2
 * DESCRIPTION: Binary integer literals with an underscore in the first position (it's considered as identifiers).
 */

// TESTCASE NUMBER: 1
val value_1 = <!UNRESOLVED_REFERENCE!>_____0b0_1_1_1_0_1<!>

// TESTCASE NUMBER: 2
val value_2 = <!UNRESOLVED_REFERENCE!>_0B1_______1_______1_______0<!>

// TESTCASE NUMBER: 3
val value_3 = <!UNRESOLVED_REFERENCE!>_0_0B1_0_1_1_1_0_1_1<!>

// TESTCASE NUMBER: 4
val value_4 = <!UNRESOLVED_REFERENCE!>_0B000000000<!>

// TESTCASE NUMBER: 5
val value_5 = <!UNRESOLVED_REFERENCE!>_0000000000b<!>

// TESTCASE NUMBER: 6
val value_6 = <!UNRESOLVED_REFERENCE!>_0_1b<!>

// TESTCASE NUMBER: 7
val value_7 = <!UNRESOLVED_REFERENCE!>____________0b<!>

// TESTCASE NUMBER: 8
val value_8 = <!UNRESOLVED_REFERENCE!>_0_b_0<!>

// TESTCASE NUMBER: 9
val value_9 = <!UNRESOLVED_REFERENCE!>_b_0<!>

// TESTCASE NUMBER: 10
val value_10 = <!UNRESOLVED_REFERENCE!>_b<!>

// TESTCASE NUMBER: 12
val value_12 = <!UNRESOLVED_REFERENCE!>_b_<!>
