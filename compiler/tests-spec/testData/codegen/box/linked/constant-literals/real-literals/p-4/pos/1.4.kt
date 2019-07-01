/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, real-literals -> paragraph 4 -> sentence 1
 * NUMBER: 4
 * DESCRIPTION: Real literals with an omitted whole-number part and underscores in a whole-number part, a fraction part and an exponent part.
 */

val value_1 = .0_0
val value_2 = .0_0f
val value_3 = .0_0e-0_0
val value_4 = .0_0e0_0F
val value_5 = .0__0F
val value_6 = .0_0E+0__0_0F

val value_7 = .0e0f
val value_8 = .0_0E0_0

val value_9 = .0e1_0F
val value_10 = .0e10__0
val value_11 = .00______00F
val value_12 = .0___9
val value_13 = .0__________________________________________________12___________________________________________________________________0F
val value_14 = .0_0e+3_0
val value_15 = .000e0f
val value_16 = .9_______9______9_____9____9___9__9_90E-1

val value_17 = .12345678_90
val value_18 = .1_2_3_4_5_6_7_8_9_0
val value_19 = .345______________6e-7_______8f
val value_20 = .45_67f
val value_21 = .5e+0_6

fun box(): String? {
    val value_22 = .6_0______________05F
    val value_23 = .76_5e4
    val value_24 = .8E7654_3
    val value_25 = .9E0_____________8765432f
    val value_26 = .09_8765432_____________1F

    val value_27 = .000000000000000000000000e-000000000000000000000000000000000000000000000000000000000000000_0F
    val value_28 = .00___000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
    val value_29 = .33333333333333333333333333333333333333333333333_333333333333333333333333333333333333333e3_3f

    if (value_1.compareTo(.0_0) != 0 || value_1.compareTo(0.0) != 0) return null
    if (value_2.compareTo(.0_0f) != 0 || value_2.compareTo(0.0) != 0) return null
    if (value_3.compareTo(.0_0e-0_0) != 0 || value_3.compareTo(0.0f) != 0) return null
    if (value_4.compareTo(.0_0e0_0F) != 0 || value_4.compareTo(0.0) != 0) return null
    if (value_5.compareTo(.0__0F) != 0 || value_5.compareTo(0.0f) != 0) return null
    if (value_6.compareTo(.0_0E+0__0_0F) != 0 || value_6.compareTo(0.0) != 0) return null

    if (value_7.compareTo(.0e0f) != 0 || value_7.compareTo(0.0f) != 0) return null
    if (value_8.compareTo(.0_0E0_0) != 0 || value_8.compareTo(0.0) != 0) return null
    if (value_9.compareTo(.0e1_0F) != 0 || value_9.compareTo(0.0F) != 0) return null
    if (value_10.compareTo(.0e10__0) != 0 || value_10.compareTo(0.0) != 0) return null
    if (value_11.compareTo(.00______00F) != 0 || value_11.compareTo(0.0F) != 0) return null
    if (value_12.compareTo(.0___9) != 0 || value_12.compareTo(0.09) != 0) return null
    if (value_13.compareTo(.0__________________________________________________12___________________________________________________________________0F) != 0 || value_13.compareTo(0.012f) != 0) return null
    if (value_14.compareTo(.0_0e+3_0) != 0 || value_14.compareTo(0.0) != 0) return null

    if (value_15.compareTo(.000e0f) != 0 || value_15.compareTo(0.0) != 0) return null
    if (value_16.compareTo(.9_______9______9_____9____9___9__9_90E-1) != 0 || value_16.compareTo(0.099999999) != 0) return null
    if (value_17.compareTo(.12345678_90) != 0 || value_17.compareTo(0.123456789) != 0) return null
    if (value_18.compareTo(.1_2_3_4_5_6_7_8_9_0) != 0 || value_18.compareTo(0.123456789) != 0) return null
    if (value_19.compareTo(.345______________6e-7_______8f) != 0 || value_19.compareTo(0.0) != 0) return null
    if (value_20.compareTo(.45_67f) != 0 || value_20.compareTo(0.4567F) != 0) return null
    if (value_21.compareTo(.5e+0_6) != 0 || value_21.compareTo(500000.0) != 0) return null
    if (value_22.compareTo(.6_0______________05F) != 0 || value_22.compareTo(0.6005f) != 0) return null
    if (value_23.compareTo(.76_5e4) != 0 || value_23.compareTo(7650.0) != 0) return null
    if (value_24.compareTo(.8E7654_3) != 0 || value_24.compareTo(Float.POSITIVE_INFINITY) != 0) return null
    if (value_25.compareTo(.9E0_____________8765432f) != 0 || value_25.compareTo(Float.POSITIVE_INFINITY) != 0) return null
    if (value_26.compareTo(.09_8765432_____________1F) != 0 || value_26.compareTo(0.09876543F) != 0) return null
    if (value_27.compareTo(.000000000000000000000000e-000000000000000000000000000000000000000000000000000000000000000_0F) != 0 || value_27.compareTo(0.0) != 0) return null
    if (value_28.compareTo(.00___000000000000000000000000000000000000000000000000000000000000000000000000000000000000000) != 0 || value_28.compareTo(0.0) != 0) return null
    if (value_29.compareTo(.33333333333333333333333333333333333333333333333_333333333333333333333333333333333333333e3_3f) != 0 || value_29.compareTo(3.3333334E32F) != 0) return null

    return "OK"
}
