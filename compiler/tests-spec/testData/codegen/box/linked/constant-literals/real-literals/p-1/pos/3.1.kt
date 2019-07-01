/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, real-literals -> paragraph 1 -> sentence 3
 * NUMBER: 1
 * DESCRIPTION: Simple real literals suffixed by f/F (the float suffix) with a different whole-number part and fraction part.
 */

val value_1 = 0.0f
val value_2 = 0.00F
val value_3 = 0.000f
val value_4 = 0.0000F

val value_5 = 00.0f
val value_6 = 000.00f
val value_7 = 0000.000F

val value_8 = 1.0F
val value_9 = 22.00F
val value_10 = 333.000F
val value_11 = 4444.0000f
val value_12 = 55555.0f
val value_13 = 666666.00f
val value_14 = 7777777.000F
val value_15 = 88888888.0000f
val value_16 = 999999999.0F

val value_17 = 0000000000.1234567890f
val value_18 = 123456789.23456789f
val value_19 = 2345678.345678F
val value_20 = 34567.4567f
val value_21 = 456.56F

fun box(): String? {
    val value_22 = 5.65F
    val value_23 = 654.7654f
    val value_24 = 76543.876543f
    val value_25 = 8765432.98765432F
    val value_26 = 987654321.0987654321f

    val value_27 = 0.1111f
    val value_28 = 1.22222f
    val value_29 = 9.33333F
    val value_30 = 9.444444F
    val value_31 = 8.5555555F
    val value_32 = 2.66666666F
    val value_33 = 3.777777777F
    val value_34 = 7.8888888888f
    val value_35 = 6.99999999999f

    if (value_1.compareTo(0.0F) != 0) return null
    if (value_2.compareTo(0.00f) != 0 || value_2.compareTo(0.0f) != 0) return null
    if (value_3.compareTo(0.000F) != 0 || value_3.compareTo(0.000f) != 0) return null
    if (value_4.compareTo(0.0000f) != 0 || value_4.compareTo(0.0F) != 0) return null
    if (value_5.compareTo(00.0f) != 0 || value_5.compareTo(0.0F) != 0) return null
    if (value_6.compareTo(000.000f) != 0 || value_6.compareTo(0.0F) != 0) return null
    if (value_7.compareTo(0000.000f) != 0 || value_7.compareTo(0.0f) != 0) return null

    if (value_8.compareTo(1.0F) != 0) return null
    if (value_9.compareTo(22.00F) != 0 || value_9.compareTo(22.0F) != 0) return null
    if (value_10.compareTo(333.000F) != 0 || value_10.compareTo(333.0f) != 0) return null
    if (value_11.compareTo(4444.0000f) != 0 || value_11.compareTo(4444.0f) != 0) return null
    if (value_12.compareTo(55555.0F) != 0) return null
    if (value_13.compareTo(666666.00F) != 0 || value_13.compareTo(666666.0f) != 0) return null
    if (value_14.compareTo(7777777.000f) != 0 || value_14.compareTo(7777777.0F) != 0) return null
    if (value_15.compareTo(88888888.0000f) != 0 || value_15.compareTo(88888888.0F) != 0) return null
    if (value_16.compareTo(999999999.0f) != 0) return null

    if (value_17.compareTo(0000000000.1234567890f) != 0 || value_17.compareTo(0.1234567890F) != 0) return null
    if (value_18.compareTo(123456789.23456789F) != 0) return null
    if (value_19.compareTo(2345678.345678F) != 0) return null
    if (value_20.compareTo(34567.4567f) != 0) return null
    if (value_21.compareTo(456.56f) != 0) return null
    if (value_22.compareTo(5.65F) != 0) return null
    if (value_23.compareTo(654.7654f) != 0) return null
    if (value_24.compareTo(76543.876543F) != 0) return null
    if (value_25.compareTo(8765432.98765432F) != 0) return null
    if (value_26.compareTo(987654321.0987654321f) != 0) return null

    if (value_27.compareTo(0.1111f) != 0) return null
    if (value_28.compareTo(1.22222f) != 0) return null
    if (value_29.compareTo(9.33333f) != 0) return null
    if (value_30.compareTo(9.444444f) != 0) return null
    if (value_31.compareTo(8.5555555F) != 0) return null
    if (value_32.compareTo(2.66666666F) != 0) return null
    if (value_33.compareTo(3.777777777f) != 0) return null
    if (value_34.compareTo(7.8888888888F) != 0) return null
    if (value_35.compareTo(6.99999999999f) != 0) return null

    return "OK"
}
