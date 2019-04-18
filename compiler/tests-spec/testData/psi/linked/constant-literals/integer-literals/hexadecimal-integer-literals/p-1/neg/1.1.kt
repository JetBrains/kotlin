/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, integer-literals, hexadecimal-integer-literals -> paragraph 1 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: Hexadecimal integer literals with invalid or double prefixes.
 */

val value = 00x876543
val value = 00l0xf876L543
val value = 0F0l0xf876L543
val value = 0F0l0xf876L543
val value = 000000000000000000000000000000000000000000000000000000xAcccccccccA
val value = 00xA45

val value = 0XXX
val value = 0xXx
val value = 0xX11111
val value = 0x0X0
val value = 0x000x
val value = 0000000000x

val value = 90X7654
val value = 09x98765432
val value = 0lX0000000
val value = 0Fx0000001000000
val value = 0e10fxEeEeEeEe
val value = 0EXAAAAAAAA
val value = 200x

val value = 0A
val value = 0z
