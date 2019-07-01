/*
 * KOTLIN PSI SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, real-literals -> paragraph 3 -> sentence 2
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
