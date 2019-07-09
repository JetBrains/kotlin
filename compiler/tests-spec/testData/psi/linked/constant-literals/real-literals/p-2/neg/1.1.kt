/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, real-literals -> paragraph 2 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: Real literals with not allowed several exponent marks.
 */

val value = 0.0ee0
val value = 0.0E-e-00
val value = 0.0eE+000
val value = 0.0e+e0000

val value = 00.0E++e0
val value = 000.00e+E-00
val value = 0000.000e-e000

val value = 1.0ee+1
val value = 22.00eE22
val value = 333.000ee-333
val value = 4444.0000e+E+e4444
val value = 55555.0eE-55555
val value = 666666.00eeeeeeeee666666
val value = 7777777.000e+E+e+E+e7777777
val value = 88888888.0000eEeEeEe-88888888
val value = 999999999.0EEEEEEEE+999999999

val value = 1.0ee+
val value = 22.00eE
val value = 333.000ee-
val value = 4444.0000e+E+
val value = 55555.0eE-
val value = 666666.00eeeeeeee
val value = 7777777.000e+E+e+E+e
val value = 88888888.0000eEeEeEe-
val value = 999999999.0EEEEEEEE+
