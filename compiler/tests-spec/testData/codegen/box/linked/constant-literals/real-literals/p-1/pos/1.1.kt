/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, real-literals -> paragraph 1 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: Simple real literals with a different whole-number part and fraction part.
 */

val value_1 = 0.0
val value_2 = 0.00
val value_3 = 0.000
val value_4 = 0.0000

val value_5 = 00.0
val value_6 = 000.00
val value_7 = 0000.000

val value_8 = 1.0
val value_9 = 22.00
val value_10 = 333.000
val value_11 = 4444.0000
val value_12 = 55555.0
val value_13 = 666666.00
val value_14 = 7777777.000
val value_15 = 88888888.0000
val value_16 = 999999999.0

val value_17 = 0000000000.1234567890
val value_18 = 123456789.23456789
val value_19 = 2345678.345678
val value_20 = 34567.4567
val value_21 = 456.56

fun box(): String? {
    val value_22 = 5.65
    val value_23 = 654.7654
    val value_24 = 76543.876543
    val value_25 = 8765432.98765432
    val value_26 = 987654321.0987654321

    val value_27 = 0.1111
    val value_28 = 1.22222
    val value_29 = 9.33333
    val value_30 = 9.444444
    val value_31 = 8.5555555
    val value_32 = 2.66666666
    val value_33 = 3.777777777
    val value_34 = 7.8888888888
    val value_35 = 6.99999999999

    if (value_1.compareTo(0.0) != 0) return null
    if (value_2.compareTo(0.00) != 0 || value_2.compareTo(0.0) != 0) return null
    if (value_3.compareTo(0.000) != 0 || value_3.compareTo(0.000) != 0) return null
    if (value_4.compareTo(0.0000) != 0 || value_4.compareTo(0.0) != 0) return null
    if (value_5.compareTo(00.0) != 0 || value_5.compareTo(0.0) != 0) return null
    if (value_6.compareTo(000.000) != 0 || value_6.compareTo(0.0) != 0) return null
    if (value_7.compareTo(0000.000) != 0 || value_7.compareTo(0.0) != 0) return null

    if (value_8.compareTo(1.0) != 0) return null
    if (value_9.compareTo(22.00) != 0 || value_9.compareTo(22.0) != 0) return null
    if (value_10.compareTo(333.000) != 0 || value_10.compareTo(333.0) != 0) return null
    if (value_11.compareTo(4444.0000) != 0 || value_11.compareTo(4444.0) != 0) return null
    if (value_12.compareTo(55555.0) != 0) return null
    if (value_13.compareTo(666666.00) != 0 || value_13.compareTo(666666.0) != 0) return null
    if (value_14.compareTo(7777777.000) != 0 || value_14.compareTo(7777777.0) != 0) return null
    if (value_15.compareTo(88888888.0000) != 0 || value_15.compareTo(88888888.0) != 0) return null
    if (value_16.compareTo(999999999.0) != 0) return null

    if (value_17.compareTo(0000000000.1234567890) != 0 || value_17.compareTo(0.1234567890) != 0) return null
    if (value_18.compareTo(123456789.23456789) != 0) return null
    if (value_19.compareTo(2345678.345678) != 0) return null
    if (value_20.compareTo(34567.4567) != 0) return null
    if (value_21.compareTo(456.56) != 0) return null
    if (value_22.compareTo(5.65) != 0) return null
    if (value_23.compareTo(654.7654) != 0) return null
    if (value_24.compareTo(76543.876543) != 0) return null
    if (value_25.compareTo(8765432.98765432) != 0) return null
    if (value_26.compareTo(987654321.0987654321) != 0) return null

    if (value_27.compareTo(0.1111) != 0) return null
    if (value_28.compareTo(1.22222) != 0) return null
    if (value_29.compareTo(9.33333) != 0) return null
    if (value_30.compareTo(9.444444) != 0) return null
    if (value_31.compareTo(8.5555555) != 0) return null
    if (value_32.compareTo(2.66666666) != 0) return null
    if (value_33.compareTo(3.777777777) != 0) return null
    if (value_34.compareTo(7.8888888888) != 0) return null
    if (value_35.compareTo(6.99999999999) != 0) return null

    return "OK"
}
