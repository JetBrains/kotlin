/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SECTIONS: constant-literals, integer-literals, long-integer-literals
 * PARAGRAPH: 1
 * SENTENCE: [1] Any of the decimal, hexadecimal or binary literals may be suffixed by the long literal mark (symbol L).
 * NUMBER: 1
 * DESCRIPTION: Binary and hexadecimal integer literals with a long literal mark only.
 */

// TESTCASE NUMBER: 1
val value_1 = <!INT_LITERAL_OUT_OF_RANGE!>0bl<!>

// TESTCASE NUMBER: 2
val value_2 = <!INT_LITERAL_OUT_OF_RANGE!>0BL<!>

// TESTCASE NUMBER: 3
val value_3 = <!INT_LITERAL_OUT_OF_RANGE!>0Xl<!>

// TESTCASE NUMBER: 4
val value_4 = <!INT_LITERAL_OUT_OF_RANGE!>0xL<!>

// TESTCASE NUMBER: 5
val value_5 = <!ILLEGAL_UNDERSCORE, INT_LITERAL_OUT_OF_RANGE!>0b_l<!>

// TESTCASE NUMBER: 6
val value_6 = <!ILLEGAL_UNDERSCORE, INT_LITERAL_OUT_OF_RANGE!>0B_L<!>

// TESTCASE NUMBER: 7
val value_7 = <!ILLEGAL_UNDERSCORE, INT_LITERAL_OUT_OF_RANGE!>0X____l<!>

// TESTCASE NUMBER: 8
val value_8 = <!ILLEGAL_UNDERSCORE, INT_LITERAL_OUT_OF_RANGE!>0x_L<!>
