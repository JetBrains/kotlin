/*
 * KOTLIN PSI SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, integer-literals, hexadecimal-integer-literals -> paragraph 1 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: Hexadecimal integer literals with valid symbols.
 */

val value = 0x1234567890
val value = 0X23456789
val value = 0x345678
val value = 0X4567
val value = 0X56
val value = 0x65
val value = 0X7654
val value = 0x876543
val value = 0x98765432

val value = 0X0
val value = 0x1

val value = 0x100000
val value = 0X1000001

val value = 0X0000000
val value = 0x0000001000000
val value = 0x00000010

val value = 0xABCDEF
val value = 0Xabcdef
val value = 0xAbcD
val value = 0Xa
val value = 0xE
val value = 0xEeEeEeEe
val value = 0XAAAAAAAA
val value = 0xcDf
val value = 0xAcccccccccA

val value = 0x0123456789ABCDEF
val value = 0x0123456789abcdef
val value = 0XAA00AA
val value = 0xBc12eF
val value = 0xa0
val value = 0XE1
val value = 0xE1eE2eE3eE4e
val value = 0XAAAAAAAA000000000
val value = 0xcDf091
val value = 0xAcccc0000cccccA
val value = 0X0000000A
val value = 0xe0000000
val value = 0x0000000D0000000
val value = 0xA0A

val value = 0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF

