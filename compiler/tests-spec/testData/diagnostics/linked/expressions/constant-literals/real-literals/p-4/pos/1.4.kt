/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-100
 * MAIN LINK: expressions, constant-literals, real-literals -> paragraph 4 -> sentence 1
 * NUMBER: 4
 * DESCRIPTION: Real literals with an omitted whole-number part and underscores in a whole-number part, a fraction part and an exponent part.
 */

// TESTCASE NUMBER: 1
val value_1 = .0_0

// TESTCASE NUMBER: 2
val value_2 = .0_0f

// TESTCASE NUMBER: 3
val value_3 = .0_0e-0_0

// TESTCASE NUMBER: 4
val value_4 = .0_0e0_0F

// TESTCASE NUMBER: 5
val value_5 = .0__0F

// TESTCASE NUMBER: 6
val value_6 = .0_0E+0__0_0F

// TESTCASE NUMBER: 7
val value_7 = .0e0f

// TESTCASE NUMBER: 8
val value_8 = .0_0E0_0

// TESTCASE NUMBER: 9
val value_9 = .0e1_0F

// TESTCASE NUMBER: 10
val value_10 = .0e10__0

// TESTCASE NUMBER: 11
val value_11 = .00______00F

// TESTCASE NUMBER: 12
val value_12 = .0___9

// TESTCASE NUMBER: 13
val value_13 = .0__________________________________________________12___________________________________________________________________0F

// TESTCASE NUMBER: 14
val value_14 = .0_0e+3_0

// TESTCASE NUMBER: 15
val value_15 = .000e0f

// TESTCASE NUMBER: 16
val value_16 = .9_______9______9_____9____9___9__9_90E-1

// TESTCASE NUMBER: 17
val value_17 = .12345678_90

// TESTCASE NUMBER: 18
val value_18 = .1_2_3_4_5_6_7_8_9_0

// TESTCASE NUMBER: 19
val value_19 = .345______________6e-7_______8f

// TESTCASE NUMBER: 20
val value_20 = .45_67f

// TESTCASE NUMBER: 21
val value_21 = .5e+0_6

// TESTCASE NUMBER: 22
val value_22 = .6_0______________05F

// TESTCASE NUMBER: 23
val value_23 = .76_5e4

// TESTCASE NUMBER: 24
val value_24 = .8E7654_3

// TESTCASE NUMBER: 25
val value_25 = .9E0_____________8765432f

// TESTCASE NUMBER: 26
val value_26 = .09_8765432_____________1F

// TESTCASE NUMBER: 27
val value_27 = .000000000000000000000000e-000000000000000000000000000000000000000000000000000000000000000_0F

// TESTCASE NUMBER: 28
val value_28 = .00___000000000000000000000000000000000000000000000000000000000000000000000000000000000000000

// TESTCASE NUMBER: 29
val value_29 = .33333333333333333333333333333333333333333333333_333333333333333333333333333333333333333e3_3f
