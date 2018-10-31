/*
 * KOTLIN PSI SPEC TEST (POSITIVE)
 *
 * SECTIONS: constant-literals, real-literals
 * PARAGRAPH: 3
 * SENTENCE: [2] The fraction part may be omitted only together with the decimal point, if the whole-number part and either the exponent part or the type suffix are present.
 * NUMBER: 3
 * DESCRIPTION: Real literals with omitted a fraction part and an exponent mark, suffixed by f/F (float suffix).
 */

val value = 0e0f
val value = 00e00F
val value = 000E-10f
val value = 0000e+00000000000f
val value = 00000000000000000000000000000000000000E1F

val value = 1e1F
val value = 22E-1f
val value = 333e-00000000000F
val value = 4444E-99999999999999999f
val value = 55555e10f
val value = 666666E00010F
val value = 7777777e+09090909090F
val value = 88888888e1234567890F
val value = 999999999E1234567890f

val value = 123456789e987654321F
val value = 2345678E0f
val value = 34567E+010f
val value = 456e-09876543210F
val value = 5e505f
val value = 654e5F
val value = 76543E-91823f
val value = 8765432e+90F
val value = 987654321e-1f
