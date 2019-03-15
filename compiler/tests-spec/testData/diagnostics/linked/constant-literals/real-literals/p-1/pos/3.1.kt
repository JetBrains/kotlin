/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, real-literals -> paragraph 1 -> sentence 3
 * NUMBER: 1
 * DESCRIPTION: Simple real literals suffixed by f/F (the float suffix) with a different whole-number part and fraction part.
 */

// TESTCASE NUMBER: 1
val value_1 = 0.0f

// TESTCASE NUMBER: 2
val value_2 = 0.00F

// TESTCASE NUMBER: 3
val value_3 = 0.000f

// TESTCASE NUMBER: 4
val value_4 = 0.0000F

// TESTCASE NUMBER: 5
val value_5 = 00.0F

// TESTCASE NUMBER: 6
val value_6 = 000.00F

// TESTCASE NUMBER: 7
val value_7 = 0000.000f

// TESTCASE NUMBER: 8
val value_8 = 1.0F

// TESTCASE NUMBER: 9
val value_9 = 22.00f

// TESTCASE NUMBER: 10
val value_10 = 333.000f

// TESTCASE NUMBER: 11
val value_11 = 4444.0000f

// TESTCASE NUMBER: 12
val value_12 = 55555.0F

// TESTCASE NUMBER: 13
val value_13 = 666666.00F

// TESTCASE NUMBER: 14
val value_14 = 7777777.000f

// TESTCASE NUMBER: 15
val value_15 = 88888888.0000f

// TESTCASE NUMBER: 16
val value_16 = 999999999.0f

// TESTCASE NUMBER: 17
val value_17 = 0000000000.1234567890F

// TESTCASE NUMBER: 18
val value_18 = 123456789.23456789F

// TESTCASE NUMBER: 19
val value_19 = 2345678.345678F

// TESTCASE NUMBER: 20
val value_20 = 34567.4567f

// TESTCASE NUMBER: 21
val value_21 = 456.56f

// TESTCASE NUMBER: 22
val value_22 = 5.65F

// TESTCASE NUMBER: 23
val value_23 = 654.7654f

// TESTCASE NUMBER: 24
val value_24 = 76543.876543f

// TESTCASE NUMBER: 25
val value_25 = 8765432.98765432F

// TESTCASE NUMBER: 26
val value_26 = 987654321.0987654321F

// TESTCASE NUMBER: 27
val value_27 = 0.1111F

// TESTCASE NUMBER: 28
val value_28 = 1.22222f

// TESTCASE NUMBER: 29
val value_29 = 9.33333f

// TESTCASE NUMBER: 30
val value_30 = 9.444444F

// TESTCASE NUMBER: 31
val value_31 = 8.5555555f

// TESTCASE NUMBER: 32
val value_32 = 2.66666666F

// TESTCASE NUMBER: 33
val value_33 = 3.777777777f

// TESTCASE NUMBER: 34
val value_34 = 7.8888888888f

// TESTCASE NUMBER: 35
val value_35 = 6.99999999999F
