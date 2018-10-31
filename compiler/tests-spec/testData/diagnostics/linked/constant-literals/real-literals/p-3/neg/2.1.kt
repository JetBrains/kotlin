/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SECTIONS: constant-literals, real-literals
 * PARAGRAPH: 3
 * SENTENCE: [2] The fraction part may be omitted only together with the decimal point, if the whole-number part and either the exponent part or the type suffix are present.
 * NUMBER: 1
 * DESCRIPTION: Real literals with omitted a fraction part and an exponent mark without digits after it.
 */

// TESTCASE NUMBER: 1
val value_1 = <!FLOAT_LITERAL_OUT_OF_RANGE!>0e<!>

// TESTCASE NUMBER: 2
val value_2 = <!FLOAT_LITERAL_OUT_OF_RANGE!>00e-<!>

// TESTCASE NUMBER: 3
val value_3 = <!FLOAT_LITERAL_OUT_OF_RANGE!>000E+<!>

// TESTCASE NUMBER: 4
val value_4 = <!FLOAT_LITERAL_OUT_OF_RANGE!>0000e+<!>

// TESTCASE NUMBER: 5
val value_5 = <!FLOAT_LITERAL_OUT_OF_RANGE!>00000000000000000000000000000000000000E<!>

// TESTCASE NUMBER: 6
val value_6 = <!FLOAT_LITERAL_OUT_OF_RANGE!>34567E+<!>

// TESTCASE NUMBER: 7
val value_7 = <!FLOAT_LITERAL_OUT_OF_RANGE!>456e-<!>

// TESTCASE NUMBER: 8
val value_8 = <!FLOAT_LITERAL_OUT_OF_RANGE!>55555e+f<!>

// TESTCASE NUMBER: 9
val value_9 = <!FLOAT_LITERAL_OUT_OF_RANGE!>666666E-F<!>
