/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, real-literals -> paragraph 3 -> sentence 2
 * NUMBER: 2
 * DESCRIPTION: Real literals with omitted a fraction part and an exponent mark.
 */

val value_1 = 0e0
val value_2 = 00e00
val value_3 = 000E-10
val value_4 = 0000e+00000000000
val value_5 = 00000000000000000000000000000000000000E1

val value_6 = 1e1
val value_7 = 22E-1
val value_8 = 333e-00000000000
val value_9 = 4444E-99999999999999999
val value_10 = 55555e10
val value_11 = 666666E00010
val value_12 = 7777777e09090909090
val value_13 = 88888888e1234567890
val value_14 = 999999999E1234567890

val value_15 = 123456789e987654321
val value_16 = 2345678E0
val value_17 = 34567E+010
val value_18 = 456e-09876543210
val value_19 = 5e505
val value_20 = 654e5
val value_21 = 76543E-91823
val value_22 = 8765432e+90
val value_23 = 987654321e-1

fun box(): String? {
    if (value_1.compareTo(0e0) != 0 || value_2.compareTo(0.0) != 0) return null
    if (value_2.compareTo(00e00) != 0 || value_2.compareTo(0.0) != 0) return null
    if (value_3.compareTo(000E-10) != 0 || value_3.compareTo(0.0) != 0) return null
    if (value_4.compareTo(0000e+00000000000) != 0 || value_4.compareTo(0.0) != 0) return null
    if (value_5.compareTo(00000000000000000000000000000000000000E1) != 0 || value_5.compareTo(0.0) != 0) return null

    if (value_6.compareTo(1e1) != 0 || value_6.compareTo(10.0) != 0) return null
    if (value_7.compareTo(22E-1) != 0 || value_7.compareTo(2.2) != 0) return null
    if (value_8.compareTo(333e-00000000000) != 0 || value_8.compareTo(333.0) != 0) return null
    if (value_9.compareTo(4444E-99999999999999999) != 0 || value_9.compareTo(0.0) != 0) return null
    if (value_10.compareTo(55555e10) != 0 || value_10.compareTo(5.5555E14) != 0) return null
    if (value_11.compareTo(666666E00010) != 0 || value_11.compareTo(6.66666e15) != 0) return null
    if (value_12.compareTo(7777777e09090909090) != 0 || value_12.compareTo(Double.POSITIVE_INFINITY) != 0) return null
    if (value_13.compareTo(88888888e1234567890) != 0 || value_13.compareTo(Double.POSITIVE_INFINITY) != 0) return null
    if (value_14.compareTo(999999999E1234567890) != 0 || value_14.compareTo(Double.POSITIVE_INFINITY) != 0) return null

    if (value_15.compareTo(123456789e987654321) != 0 || value_15.compareTo(Double.POSITIVE_INFINITY) != 0) return null
    if (value_16.compareTo(2345678E0) != 0 || value_16.compareTo(2345678.0) != 0) return null
    if (value_17.compareTo(34567E+010) != 0 || value_17.compareTo(3.4567E14) != 0) return null
    if (value_18.compareTo(456e-09876543210) != 0 || value_18.compareTo(0.0) != 0) return null
    if (value_19.compareTo(5e505) != 0 || value_19.compareTo(Double.POSITIVE_INFINITY) != 0) return null
    if (value_20.compareTo(654e5) != 0 || value_20.compareTo(6.54E7) != 0) return null
    if (value_21.compareTo(76543E-91823) != 0 || value_21.compareTo(0.0) != 0) return null
    if (value_22.compareTo(8765432e+90) != 0 || value_22.compareTo(8.765432E96) != 0) return null
    if (value_23.compareTo(987654321e-1) != 0 || value_23.compareTo(9.87654321E7) != 0) return null

    return "OK"
}
