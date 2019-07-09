/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, integer-literals, long-integer-literals -> paragraph 1 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: Various integer literals with a long literal mark in not allowed places.
 */

val value = 0x0123456L789abcdef
val value = 0lXal
val value = 0xL0L
val value = 0X4_______5_______d_______L7l
val value = 0xl_l
val value = 0b10101010101L0
val value = 0bL000000009l
val value = 0LB1___L____0____l___1____L___0
val value = 0Bl1______________0
val value = 0L0L
val value = 1L0L
