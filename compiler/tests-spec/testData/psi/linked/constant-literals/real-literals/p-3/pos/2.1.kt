/*
 * KOTLIN PSI SPEC TEST (POSITIVE)
 *
 * SECTIONS: constant-literals, real-literals
 * PARAGRAPH: 3
 * SENTENCE: [2] The fraction part may be omitted only together with the decimal point, if the whole-number part and either the exponent part or the type suffix are present.
 * NUMBER: 1
 * DESCRIPTION: Simple real literals with omitted a fraction part, suffixed by f/F (float suffix).
 */

val value = 0F
val value = 00F
val value = 000f
val value = 0000F
val value = 00000000000000000000000000000000000000f

val value = 1f
val value = 22f
val value = 333F
val value = 4444f
val value = 55555F
val value = 666666f
val value = 7777777f
val value = 88888888F
val value = 999999999F

val value = 123456789f
val value = 2345678F
val value = 34567F
val value = 456F
val value = 5f
val value = 654F
val value = 76543F
val value = 8765432f
val value = 987654321F
