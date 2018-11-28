/*
 * KOTLIN PSI SPEC TEST (POSITIVE)
 *
 * SECTIONS: constant-literals, real-literals
 * PARAGRAPH: 3
 * SENTENCE: [2] The fraction part may be omitted only together with the decimal point, if the whole-number part and either the exponent part or the type suffix are present.
 * NUMBER: 2
 * DESCRIPTION: Real literals with omitted a fraction part and an exponent mark.
 */

val value = 0e0
val value = 00e00
val value = 000E-10
val value = 0000e+00000000000
val value = 00000000000000000000000000000000000000E1

val value = 1e1
val value = 22E-1
val value = 333e-00000000000
val value = 4444E-99999999999999999
val value = 55555e10
val value = 666666E00010
val value = 7777777e09090909090
val value = 88888888e1234567890
val value = 999999999E1234567890

val value = 123456789e987654321
val value = 2345678E0
val value = 34567E+010
val value = 456e-09876543210
val value = 5e505
val value = 654e5
val value = 76543E-91823
val value = 8765432e+90
val value = 987654321e-1
