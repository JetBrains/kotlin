/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, real-literals -> paragraph 2 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: Real literals suffixed by f/F (the float suffix) with not allowed several exponent marks.
 */

val value = 0.0ee0f
val value = 0.0E-e-00F
val value = 0.0eE+000f
val value = 0.0e+e0000F

val value = 00.0E++e0f
val value = 000.00e+E-00f
val value = 0000.000e-e000f

val value = 1.0ee+1F
val value = 22.00eE22F
val value = 333.000ee-333F
val value = 4444.0000e+E+e4444f
val value = 55555.0eE-55555f
val value = 666666.00eeeeeeeee666666f
val value = 7777777.000e+E+e+E+e7777777F
val value = 88888888.0000eEeEeEe-88888888f
val value = 999999999.0EEEEEEEE+999999999F

val value = 1.0ee+f
val value = 22.00eEf
val value = 333.000ee-F
val value = 4444.0000e+E+f
val value = 55555.0eE-f
val value = 666666.00eeeeeeeeF
val value = 7777777.000e+E+e+E+eF
val value = 88888888.0000eEeEeEe-f
val value = 999999999.0EEEEEEEE+F
