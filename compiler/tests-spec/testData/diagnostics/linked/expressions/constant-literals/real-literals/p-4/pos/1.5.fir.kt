/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-100
 * MAIN LINK: expressions, constant-literals, real-literals -> paragraph 4 -> sentence 1
 * NUMBER: 5
 * DESCRIPTION: Real literals with an omitted fraction part and underscores in a whole-number part, a fraction part and an exponent part.
 */

// TESTCASE NUMBER: 1
val value_1 = 0_0F

// TESTCASE NUMBER: 2
val value_2 = 0_0E-0_0F

// TESTCASE NUMBER: 3
val value_3 = 0_0E-0_0

// TESTCASE NUMBER: 4
val value_4 = 0_0____0f

// TESTCASE NUMBER: 5
val value_5 = 0_0____0e-0f

// TESTCASE NUMBER: 6
val value_6 = 0_0_0_0F

// TESTCASE NUMBER: 7
val value_7 = 0_0_0_0E-0_0_0_0F

// TESTCASE NUMBER: 8
val value_8 = 0000000000000000000_______________0000000000000000000f

// TESTCASE NUMBER: 9
val value_9 = 0000000000000000000_______________0000000000000000000e+0f

// TESTCASE NUMBER: 10
val value_10 = 0000000000000000000_______________0000000000000000000E-0

// TESTCASE NUMBER: 11
val value_11 = 2___2e-2___2f

// TESTCASE NUMBER: 12
val value_12 = 33_3E0_0F

// TESTCASE NUMBER: 13
val value_13 = 4_444E-4_444f

// TESTCASE NUMBER: 14
val value_14 = 55_5_55F

// TESTCASE NUMBER: 15
val value_15 = 666____________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________666f

// TESTCASE NUMBER: 16
val value_16 = 666____________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________666E-10

// TESTCASE NUMBER: 17
val value_17 = 7_7_7_7_7_7_7f

// TESTCASE NUMBER: 18
val value_18 = 8888888________8e-9000000_0

// TESTCASE NUMBER: 19
val value_19 = 9________9_______9______9_____9____9___9__9_9F

// TESTCASE NUMBER: 20
val value_20 = 1__2_3__4____5_____6__7_89f

// TESTCASE NUMBER: 21
val value_21 = 2__34567e8

// TESTCASE NUMBER: 22
val value_22 = 345_6E+9_7F

// TESTCASE NUMBER: 23
val value_23 = 45_____________________________________________________________6E-12313413_4

// TESTCASE NUMBER: 24
val value_24 = 5_______________________________________________________________________________________________________________________________________________________________________________________5f

// TESTCASE NUMBER: 25
val value_25 = 6__________________________________________________54F

// TESTCASE NUMBER: 26
val value_26 = 76_5___4e3___________33333333

// TESTCASE NUMBER: 27
val value_27 = 876543_____________________________________________________________2f

// TESTCASE NUMBER: 28
val value_28 = 9_8__7654__3_21F

// TESTCASE NUMBER: 29
val value_29 = 000000000000000000000000000000000000000000000000000000000000000000000000000000000000000e0__0F

// TESTCASE NUMBER: 30
val value_30 = 0___000000000000000000000000000000000000000000000000000000000000000000000000000000000000000f

// TESTCASE NUMBER: 31
val value_31 = 33333333333333333333333333333333333333333333333_33333333333333333333333333333333333333333E-1_0_0
