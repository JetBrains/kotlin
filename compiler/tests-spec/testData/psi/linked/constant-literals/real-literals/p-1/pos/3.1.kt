/*
 * KOTLIN PSI SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, real-literals -> paragraph 1 -> sentence 3
 * NUMBER: 1
 * DESCRIPTION: Simple real literals suffixed by f/F (the float suffix) with a different whole-number part and fraction part.
 */

val value = 0.0f
val value = 0.00F
val value = 0.000f
val value = 0.0000F

val value = 00.0F
val value = 000.00F
val value = 0000.000f

val value = 1.0F
val value = 22.00f
val value = 333.000f
val value = 4444.0000f
val value = 55555.0F
val value = 666666.00F
val value = 7777777.000f
val value = 88888888.0000f
val value = 999999999.0f

val value = 0000000000.1234567890F
val value = 123456789.23456789F
val value = 2345678.345678F
val value = 34567.4567f
val value = 456.56f
val value = 5.65F
val value = 654.7654f
val value = 76543.876543f
val value = 8765432.98765432F
val value = 987654321.0987654321F

val value = 0.1111F
val value = 1.22222f
val value = 9.33333f
val value = 9.444444F
val value = 8.5555555f
val value = 2.66666666F
val value = 3.777777777f
val value = 7.8888888888f
val value = 6.99999999999F
