/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SECTIONS: constant-literals, integer-literals, decimal-integer-literals
 * PARAGRAPH: 1
 * SENTENCE: [2] Digits may be separated by an underscore symbol, but no underscore can be placed before the first digit or after the last one.
 * NUMBER: 1
 * DESCRIPTION: Integer literals with an underscore in the last position.
 */

// TESTCASE NUMBER: 1
val value_1 = <!ILLEGAL_UNDERSCORE!>1_<!>

// TESTCASE NUMBER: 2
val value_2 = <!ILLEGAL_UNDERSCORE!>1_00000000000000000_<!>

// TESTCASE NUMBER: 3
val value_3 = <!ILLEGAL_UNDERSCORE!>1_____________<!>

// TESTCASE NUMBER: 4
val value_4 = <!ILLEGAL_UNDERSCORE!>9____________0_<!>

// TESTCASE NUMBER: 5
val value_5 = <!ILLEGAL_UNDERSCORE!>1_______________________________________________________________________________________________________________________________________________________<!>
