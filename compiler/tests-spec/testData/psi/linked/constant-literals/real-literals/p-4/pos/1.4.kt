/*
 * KOTLIN PSI SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, real-literals -> paragraph 4 -> sentence 1
 * NUMBER: 4
 * DESCRIPTION: Real literals with an omitted whole-number part and underscores in a whole-number part, a fraction part and an exponent part.
 */

val value = .0_0
val value = .0_0f
val value = .0_0e-0_0
val value = .0_0e0_0F
val value = .0__0F
val value = .0_0E+0__0_0F

val value = .0e0f
val value = .0_0E0_0

val value = .0e1_0F
val value = .0e10__0
val value = .00______00F
val value = .0___9
val value = .0__________________________________________________12___________________________________________________________________0F
val value = .0_0e+3_0
val value = .000e0f
val value = .9_______9______9_____9____9___9__9_90E-1

val value = .12345678_90
val value = .1_2_3_4_5_6_7_8_9_0
val value = .345______________6e-7_______8f
val value = .45_67f
val value = .5e+0_6
val value = .6_0______________05F
val value = .76_5e4
val value = .8E7654_3
val value = .9E0_____________8765432f
val value = .09_8765432_____________1F

val value = .000000000000000000000000e-000000000000000000000000000000000000000000000000000000000000000_0F
val value = .00___000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
val value = .33333333333333333333333333333333333333333333333_333333333333333333333333333333333333333e3_3f
