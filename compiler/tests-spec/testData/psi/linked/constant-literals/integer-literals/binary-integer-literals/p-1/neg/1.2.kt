/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, integer-literals, binary-integer-literals -> paragraph 1 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: Binary integer literals with not allowed symbols.
 */

val value = 0b101L001
val value = 0bf101L001
val value = 0bb1110010110
val value = 0Bb11001011
val value = 0bB100101
val value = 0B00b10
val value = 0B00B1
val value = 0b10b0
val value = 0bBb0100
val value = 0BG
val value = 0bF1z
val value = 0b100M000
val value = 0BBBB1000001
val value = 0b00000010b
val value = 0bABCDEFBB
val value = 0Babcdefghijklmnopqrstuvwbyz
val value = 0BABCDEFGHIJKLMNOPQRSTUVWBYZ
val value = 0Bа
val value = 0b10С10
val value = 0beeeeеееее
val value = 0bbbbbbb
val value = 0B0BBBBBB
val value = 0B0b0
val value = 0bAF
