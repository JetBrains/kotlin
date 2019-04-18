/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, integer-literals, binary-integer-literals -> paragraph 1 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: Binary integer literals with invalid or double prefixes.
 */

val value = 00b000111001
val value = 00l0b10001110
val value = 0F0l0b11111111
val value = 0F0l0b00000000
val value = 000000000000000000000000000000000000000000000000000000b001101
val value = 00b01
val value = 10B1001
val value = 01b0001110
val value = 0lB0000000
val value = 0Fb0000001000000
val value = 0e10fb11010001
val value = 0EB1100001
val value = 100b

val value = 0BBB
val value = 0bBb
val value = 0bB11111
val value = 0b0B0
val value = 0b000b
val value = 0000000000b

val value = 0A
val value = 0z
