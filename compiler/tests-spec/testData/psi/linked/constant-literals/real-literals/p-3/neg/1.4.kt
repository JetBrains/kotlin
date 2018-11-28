/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SECTIONS: constant-literals, real-literals
 * PARAGRAPH: 3
 * SENTENCE: [1] The whole-number part and the exponent part may be omitted.
 * NUMBER: 4
 * DESCRIPTION: Real literals with omitted whole-number part and an exponent mark followed by a float suffix.
 */

val value = .0fe10
val value = .0F-e00
val value = .0fEe+000
val value = .0Fe+0000

val value = .0fe+0
val value = .00Fe00
val value = .000fFEe-000

val value = .0Fe+1
val value = .00ffee22
val value = .000Fe-0
val value = .0000fFe4444
val value = .0Fee-55555
val value = .00FeE+666666
