// FIR_IDENTICAL
/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-100
 * MAIN LINK: expressions, constant-literals, integer-literals, decimal-integer-literals -> paragraph 1 -> sentence 2
 * NUMBER: 3
 * DESCRIPTION: Integer literals with an underscore in the first position (it's considered as identifiers).
 */

// TESTCASE NUMBER: 1
val value_1 = <!UNRESOLVED_REFERENCE!>_5678_90<!>

// TESTCASE NUMBER: 2
val value_2 = <!UNRESOLVED_REFERENCE!>_2_3_4_5_6_7_8_9_<!>

// TESTCASE NUMBER: 3
val value_3 = <!UNRESOLVED_REFERENCE!>_____________0000<!>

// TESTCASE NUMBER: 4
val value_4 = <!UNRESOLVED_REFERENCE!>_______________________________________________________________________________________________________________________________________________________0<!>

// TESTCASE NUMBER: 5
val value_5 = <!UNRESOLVED_REFERENCE!>____________________________________________________<!>

// TESTCASE NUMBER: 6
val value_6 = <!UNRESOLVED_REFERENCE!>_<!>

// TESTCASE NUMBER: 7
val value_7 = <!UNRESOLVED_REFERENCE!>_0_<!>

// TESTCASE NUMBER: 8
val value_8 = <!UNRESOLVED_REFERENCE!>_9_<!>
