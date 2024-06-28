/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-435
 * MAIN LINK: expressions, constant-literals, real-literals -> paragraph 2 -> sentence 1
 * NUMBER: 3
 * DESCRIPTION: Real literals suffixed by f/F (float suffix) with a not allowed exponent mark at the beginning.
 */

// TESTCASE NUMBER: 1
val value_1 = <!UNRESOLVED_REFERENCE!>E0f<!>

// TESTCASE NUMBER: 2
val value_2 = <!UNRESOLVED_REFERENCE!>e000F<!>

// TESTCASE NUMBER: 3
val value_3 = <!UNRESOLVED_REFERENCE!>E<!>+0f

// TESTCASE NUMBER: 4
val value_4 = <!UNRESOLVED_REFERENCE!>e00f<!>

// TESTCASE NUMBER: 5
val value_5 = <!UNRESOLVED_REFERENCE!>e<!>+1F

// TESTCASE NUMBER: 6
val value_6 = <!UNRESOLVED_REFERENCE!>e22F<!>

// TESTCASE NUMBER: 7
val value_7 = <!UNRESOLVED_REFERENCE!>E<!>-333F

// TESTCASE NUMBER: 8
val value_8 = <!UNRESOLVED_REFERENCE!>e4444f<!>

// TESTCASE NUMBER: 9
val value_9 = <!UNRESOLVED_REFERENCE!>e<!>-55555f

// TESTCASE NUMBER: 10
val value_10 = <!UNRESOLVED_REFERENCE!>e666666F<!>

// TESTCASE NUMBER: 11
val value_11 = <!UNRESOLVED_REFERENCE!>E7777777f<!>

// TESTCASE NUMBER: 12
val value_12 = <!UNRESOLVED_REFERENCE!>e<!>-88888888F

// TESTCASE NUMBER: 13
val value_13 = <!UNRESOLVED_REFERENCE!>E<!>+999999999F
