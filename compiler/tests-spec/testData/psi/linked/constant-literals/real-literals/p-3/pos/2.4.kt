/*
 * KOTLIN PSI SPEC TEST (POSITIVE)
 *
 * SECTIONS: constant-literals, real-literals
 * PARAGRAPH: 3
 * SENTENCE: [2] The fraction part may be omitted only together with the decimal point, if the whole-number part and either the exponent part or the type suffix are present.
 * NUMBER: 4
 * DESCRIPTION: Real literals with omitted a fraction part and an exponent mark without digits after it.
 */

val value = 0e
val value = 00e-
val value = 000E+
val value = 0000e+
val value = 00000000000000000000000000000000000000E
val value = 34567E+
val value = 456e-
val value = 55555e+f
val value = 666666E-F
