/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, real-literals -> paragraph 3 -> sentence 1
 * NUMBER: 4
 * DESCRIPTION: Real literals suffixed by f/F (float suffix) with omitted a whole-number part.
 */

// TESTCASE NUMBER: 1
val value_1 = .0F

// TESTCASE NUMBER: 2
val value_2 = .00F

// TESTCASE NUMBER: 3
val value_3 = .000F

// TESTCASE NUMBER: 4
val value_4 = .0000f

// TESTCASE NUMBER: 5
val value_5 = .1234567890f

// TESTCASE NUMBER: 6
val value_6 = .23456789f

// TESTCASE NUMBER: 7
val value_7 = .345678F

// TESTCASE NUMBER: 8
val value_8 = .4567f

// TESTCASE NUMBER: 9
val value_9 = .56F

// TESTCASE NUMBER: 10
val value_10 = .65F

// TESTCASE NUMBER: 11
val value_11 = .7654f

// TESTCASE NUMBER: 12
val value_12 = .876543f

// TESTCASE NUMBER: 13
val value_13 = .98765432F

// TESTCASE NUMBER: 14
val value_14 = .0987654321f

// TESTCASE NUMBER: 15
val value_15 = .1111f

// TESTCASE NUMBER: 16
val value_16 = .22222f

// TESTCASE NUMBER: 17
val value_17 = .33333F

// TESTCASE NUMBER: 18
val value_18 = .444444F

// TESTCASE NUMBER: 19
val value_19 = .5555555F

// TESTCASE NUMBER: 20
val value_20 = .66666666F

// TESTCASE NUMBER: 21
val value_21 = .777777777F

// TESTCASE NUMBER: 22
val value_22 = .8888888888f

// TESTCASE NUMBER: 23
val value_23 = .99999999999f
