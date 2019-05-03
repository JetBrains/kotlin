/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, real-literals -> paragraph 3 -> sentence 1
 * NUMBER: 3
 * DESCRIPTION: Real literals suffixed by f/F (float suffix) with omitted whole-number part and not allowed several exponent marks.
 */

val value = .0ee0
val value = .0E-e-00F
val value = .0eE+000
val value = .0e+e0000f

val value = .0E++e0f
val value = .00e+E-00F
val value = .000e-e000

val value = .0ee+1
val value = .00eE22
val value = .000ee-333f
val value = .0000e+E+e4444
val value = .0eE-55555
val value = .00eeeeeeeee666666F
val value = .000e+E+e+E+e7777777
val value = .0000eEeEeEe-88888888
val value = .0EEEEEEEE+999999999

val value = .0ee+F
val value = .00eE
val value = .000ee-f
val value = .0000e+E+
val value = .0eE-f
val value = .00eeeeeeeef
val value = .000e+E+e+E+e
val value = .0000eEeEeEe-
val value = .0EEEEEEEE+F
