/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-100
 * MAIN LINK: expressions, constant-literals, real-literals -> paragraph 5 -> sentence 1
 * NUMBER: 5
 * DESCRIPTION: A type checking of a real literal with omitted a fraction part and an exponent mark.
 * HELPERS: checkType
 */

// TESTCASE NUMBER: 1
val value_1 = 0e0 checkType { check<Double>() }

// TESTCASE NUMBER: 2
val value_2 = 0_0e0_0 checkType { check<Double>() }

// TESTCASE NUMBER: 3
val value_3 = 000E-10 checkType { check<Double>() }

// TESTCASE NUMBER: 4
val value_4 = 00______00e+00000000000 checkType { check<Double>() }

// TESTCASE NUMBER: 5
val value_5 = 0000000000000000000000000000000000000_0E1 checkType { check<Double>() }

// TESTCASE NUMBER: 6
val value_6 = 1e1 checkType { check<Double>() }

// TESTCASE NUMBER: 7
val value_7 = 2___2E-1 checkType { check<Double>() }

// TESTCASE NUMBER: 8
val value_8 = 333e-00000000000 checkType { check<Double>() }

// TESTCASE NUMBER: 9
val value_9 = 4444E-99999999999999999 checkType { check<Double>() }

// TESTCASE NUMBER: 10
val value_10 = 5_5_5_5_5e10 checkType { check<Double>() }

// TESTCASE NUMBER: 11
val value_11 = 666666E0_0_0_1_0 checkType { check<Double>() }

// TESTCASE NUMBER: 12
val value_12 = 7777777e09090909090 checkType { check<Double>() }

// TESTCASE NUMBER: 13
val value_13 = 88888888e1234567890 checkType { check<Double>() }

// TESTCASE NUMBER: 14
val value_14 = 999999999E1234567890 checkType { check<Double>() }

// TESTCASE NUMBER: 15
val value_15 = 123456789e9_____87___654__32_1 checkType { check<Double>() }

// TESTCASE NUMBER: 16
val value_16 = 2345678E0 checkType { check<Double>() }

// TESTCASE NUMBER: 17
val value_17 = 3____4___5__6_7E+010 checkType { check<Double>() }

// TESTCASE NUMBER: 18
val value_18 = 456e-09876543210 checkType { check<Double>() }

// TESTCASE NUMBER: 19
val value_19 = 5e5_0_5 checkType { check<Double>() }

// TESTCASE NUMBER: 20
val value_20 = 654e5 checkType { check<Double>() }

// TESTCASE NUMBER: 21
val value_21 = 76543E-91823 checkType { check<Double>() }

// TESTCASE NUMBER: 22
val value_22 = 8765432e+9_______0 checkType { check<Double>() }

// TESTCASE NUMBER: 23
val value_23 = 9_87654321e-1 checkType { check<Double>() }
