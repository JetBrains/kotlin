/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, real-literals -> paragraph 6 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: Real literals suffixed by not supported d/D (a double suffix).
 */

val value = 0.0d
val value = 0.00d
val value = 0000.000D

val value = 1.0d
val value = 22.00D

val value = 0.0e0d
val value = 0.0e-00D
val value = 0.0E+0000D
val value = 0000.000E-000d

val value = 1.0E+1d
val value = 333.000e-333d
val value = 123456789.23456789E+123456789D

val value = .0d
val value = .0000d
val value = .1234567890D
val value = .9999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999123456789912345678991234567899123456789912345678991234567899D

val value = 0d
val value = 00000000000000000000000000000000000000D
val value = 4444d
val value = 987654321d
val value = 0e0D
val value = 00e00D
val value = 000E-10d
val value = 0000e+00000000000d
val value = 00000000000000000000000000000000000000E1D

val value = 00e-00D
val value = 1e1d
val value = 333e-00000000000d
val value = 88888888e1234567890D

val value = 0.0__0___0D
val value = 0_0_0_0E-0_0_0_0d
val value = .0_0E+0__0_0D
val value = 0_0_0.0_0E0_0d
val value = 666_666.0_____________________________________________________________________________________________________________________0d
val value = 9_______9______9_____9____9___9__9_9.0E-1D
val value = .0e-9_8765432_____________1D
val value = 45_____________________________________________________________6E-12313413_4d
