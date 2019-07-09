/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, real-literals -> paragraph 4 -> sentence 1
 * NUMBER: 3
 * DESCRIPTION: Real literals suffixed by f/F (float suffix) with an exponent mark and underscores in a whole-number part, a fraction part and an exponent part.
 */

val value_1 = 0.0_0e1_0f
val value_2 = 0.0__0e-0___0
val value_3 = 0.0_0E-0__0_0F
val value_4 = 0__0.0e0f
val value_5 = 0_0_0.0_0E0_0
val value_6 = 00_______________00.0_0e+0_0

val value_7 = 2_2.0e1_0F
val value_8 = 33__3.0e10__0
val value_9 = 4_44____4.0E0______00F
val value_10 = 5_________555_________5.0e-9
val value_11 = 666_666.0__________________________________________________1E+2___________________________________________________________________0F
val value_12 = 7777777.0_0e3_0
val value_13 = 8888888_8.000e0f
val value_14 = 9_______9______9_____9____9___9__9_9.0E-1

val value_15 = 0_0_0_0_0_0_0_0_0_0.12345678e+90F
val value_16 = 1_2_3_4_5_6_7_8_9.2_3_4_5_6_7_8_9e-0

fun box(): String? {
    val value_17 = 234_5_678.345______________6e7_______8f
    val value_18 = 3_456_7.45_6E7f
    val value_19 = 456.5e0_6
    val value_20 = 5.6_0E+05F
    val value_21 = 6_54.76_5e-4
    val value_22 = 7_6543.8E7654_3
    val value_23 = 876543_____________2.9E+0_____________8765432f
    val value_24 = 9_____________87654321.0e-9_8765432_____________1F

    val value_25 = 000000000000000000000000000000000000000000000000000000000000000000000000000000000000000___0.000000000000000000000000e000000000000000000000000000000000000000000000000000000000000000_0F
    val value_26 = 0_000000000000000000000000000000000000000000000000000000000000000000000000000000000000000.0E-0___000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
    val value_27 = 9999999999999999999999999999999999999999999_______________999999999999999999999999999999999999999999999.33333333333333333333333333333333333333333333333_333333333333333333333333333333333333333e3_3f

    if (value_1.compareTo(0.0_0e1_0f) != 0 || value_1.compareTo(0.0f) != 0) return null
    if (value_2.compareTo(0.0__0e-0___0) != 0 || value_2.compareTo(0.0) != 0) return null
    if (value_3.compareTo(0.0_0E-0__0_0F) != 0 || value_3.compareTo(0.0f) != 0) return null
    if (value_4.compareTo(0__0.0e0f) != 0 || value_4.compareTo(0.0) != 0) return null
    if (value_5.compareTo(0_0_0.0_0E0_0) != 0 || value_5.compareTo(0.0f) != 0) return null
    if (value_6.compareTo(00_______________00.0_0e+0_0) != 0 || value_6.compareTo(0.0f) != 0) return null

    if (value_7.compareTo(2_2.0e1_0F) != 0 || value_7.compareTo(2.19999994E11f) != 0) return null
    if (value_8.compareTo(33__3.0e10__0) != 0 || value_8.compareTo(3.33E102) != 0) return null
    if (value_9.compareTo(4_44____4.0E0______00F) != 0 || value_9.compareTo(4444.0f) != 0) return null
    if (value_10.compareTo(5_________555_________5.0e-9) != 0 || value_10.compareTo(5.5555E-5) != 0) return null
    if (value_11.compareTo(666_666.0__________________________________________________1E+2___________________________________________________________________0F) != 0 || value_11.compareTo(6.66666E25F) != 0) return null
    if (value_12.compareTo(7777777.0_0e3_0) != 0 || value_12.compareTo(7.777777E36) != 0) return null
    if (value_13.compareTo(8888888_8.000e0f) != 0 || value_13.compareTo(8.8888888E7) != 0) return null
    if (value_14.compareTo(9_______9______9_____9____9___9__9_9.0E-1) != 0 || value_14.compareTo(9999999.9) != 0) return null

    if (value_15.compareTo(0_0_0_0_0_0_0_0_0_0.12345678e+90F) != 0 || value_15.compareTo(Float.POSITIVE_INFINITY) != 0) return null
    if (value_16.compareTo(1_2_3_4_5_6_7_8_9.2_3_4_5_6_7_8_9e-0) != 0 || value_16.compareTo(1.234567892345679E8) != 0) return null
    if (value_17.compareTo(234_5_678.345______________6e7_______8f) != 0 || value_17.compareTo(Float.POSITIVE_INFINITY) != 0) return null
    if (value_18.compareTo(3_456_7.45_6E7f) != 0 || value_18.compareTo(3.45674547E11F) != 0) return null
    if (value_19.compareTo(456.5e0_6) != 0 || value_19.compareTo(4.565E8f) != 0) return null
    if (value_20.compareTo(5.6_0E+05F) != 0 || value_20.compareTo(560000.0F) != 0) return null
    if (value_21.compareTo(6_54.76_5e-4) != 0 || value_21.compareTo(0.0654765) != 0) return null
    if (value_22.compareTo(7_6543.8E7654_3) != 0 || value_22.compareTo(Double.POSITIVE_INFINITY) != 0) return null
    if (value_23.compareTo(876543_____________2.9E+0_____________8765432f) != 0 || value_23.compareTo(Float.POSITIVE_INFINITY) != 0) return null
    if (value_24.compareTo(9_____________87654321.0e-9_8765432_____________1F) != 0 || value_24.compareTo(0.0) != 0) return null
    if (value_25.compareTo(000000000000000000000000000000000000000000000000000000000000000000000000000000000000000___0.000000000000000000000000e000000000000000000000000000000000000000000000000000000000000000_0F) != 0 || value_25.compareTo(0.0) != 0) return null
    if (value_26.compareTo(0_000000000000000000000000000000000000000000000000000000000000000000000000000000000000000.0E-0___000000000000000000000000000000000000000000000000000000000000000000000000000000000000000) != 0 || value_26.compareTo(0.0) != 0) return null
    if (value_27.compareTo(9999999999999999999999999999999999999999999_______________999999999999999999999999999999999999999999999.33333333333333333333333333333333333333333333333_333333333333333333333333333333333333333e3_3f) != 0 || value_27.compareTo(Float.POSITIVE_INFINITY) != 0) return null

    return "OK"
}
