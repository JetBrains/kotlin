/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, real-literals -> paragraph 3 -> sentence 2
 * NUMBER: 3
 * DESCRIPTION: Real literals with omitted a fraction part and an exponent mark, suffixed by f/F (float suffix).
 */

val value_1 = 0e0f
val value_2 = 00e00F
val value_3 = 000E-10f
val value_4 = 0000e+00000000000f
val value_5 = 00000000000000000000000000000000000000E1F

val value_6 = 1e1F
val value_7 = 22E-1f
val value_8 = 333e-00000000000F
val value_9 = 4444E-99999999999999999f
val value_10 = 55555e10f
val value_11 = 666666E00010F
val value_12 = 7777777e09090909090F
val value_13 = 88888888e1234567890F
val value_14 = 999999999E1234567890f

val value_15 = 123456789e987654321F
val value_16 = 2345678E0f
val value_17 = 34567E+010f
val value_18 = 456e-09876543210F
val value_19 = 5e505f
val value_20 = 654e5F
val value_21 = 76543E-91823f
val value_22 = 8765432e+90F
val value_23 = 987654321e-1f

fun box(): String? {
    if (value_1.compareTo(0e0f) != 0 || value_2.compareTo(0.0f) != 0) return null
    if (value_2.compareTo(00e00F) != 0 || value_2.compareTo(0.0F) != 0) return null
    if (value_3.compareTo(000E-10F) != 0 || value_3.compareTo(0.0f) != 0) return null
    if (value_4.compareTo(0000e+00000000000F) != 0 || value_4.compareTo(0.0f) != 0) return null
    if (value_5.compareTo(00000000000000000000000000000000000000E1f) != 0 || value_5.compareTo(0.0f) != 0) return null

    if (value_6.compareTo(1e1f) != 0 || value_6.compareTo(10.0F) != 0) return null
    if (value_7.compareTo(22E-1F) != 0 || value_7.compareTo(2.2f) != 0) return null
    if (value_8.compareTo(333e-00000000000F) != 0 || value_8.compareTo(333.0F) != 0) return null
    if (value_9.compareTo(4444E-99999999999999999F) != 0 || value_9.compareTo(0.0f) != 0) return null
    if (value_10.compareTo(55555e10F) != 0 || value_10.compareTo(5.5555E14f) != 0) return null
    if (value_11.compareTo(666666E00010F) != 0 || value_11.compareTo(6.66666e15f) != 0) return null
    if (value_12.compareTo(7777777e09090909090f) != 0 || value_12.compareTo(Float.POSITIVE_INFINITY) != 0) return null
    if (value_13.compareTo(88888888e1234567890F) != 0 || value_13.compareTo(Float.POSITIVE_INFINITY) != 0) return null
    if (value_14.compareTo(999999999E1234567890f) != 0 || value_14.compareTo(Float.POSITIVE_INFINITY) != 0) return null

    if (value_15.compareTo(123456789e987654321f) != 0 || value_15.compareTo(Float.POSITIVE_INFINITY) != 0) return null
    if (value_16.compareTo(2345678E0F) != 0 || value_16.compareTo(2345678.0F) != 0) return null
    if (value_17.compareTo(34567E+010F) != 0 || value_17.compareTo(3.4567E14f) != 0) return null
    if (value_18.compareTo(456e-09876543210F) != 0 || value_18.compareTo(0.0f) != 0) return null
    if (value_19.compareTo(5e505f) != 0 || value_19.compareTo(Float.POSITIVE_INFINITY) != 0) return null
    if (value_20.compareTo(654e5f) != 0 || value_20.compareTo(6.54E7F) != 0) return null
    if (value_21.compareTo(76543E-91823f) != 0 || value_21.compareTo(0.0f) != 0) return null
    if (value_22.compareTo(8765432e+90F) != 0 || value_22.compareTo(8.765432E96F) != 0) return null
    if (value_23.compareTo(987654321e-1f) != 0 || value_23.compareTo(9.87654321E7F) != 0) return null

    return "OK"
}
