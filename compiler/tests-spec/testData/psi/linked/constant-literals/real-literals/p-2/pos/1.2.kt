/*
 * KOTLIN PSI SPEC TEST (POSITIVE)
 *
 * SECTIONS: constant-literals, real-literals
 * PARAGRAPH: 2
 * SENTENCE: [1] The exponent is an exponent mark (e or E) followed by an optionaly signed decimal integer (a sequence of decimal digits).
 * NUMBER: 2
 * DESCRIPTION: Real literals with a not allowed exponent mark at the beginning.
 */

val value = E0
val value = e000

val value = E+0
val value = e00

val value = e+1
val value = e22
val value = E-333
val value = e4444
val value = e-55555
val value = e666666
val value = E7777777
val value = e-88888888
val value = E+999999999
