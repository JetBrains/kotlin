/*
 * KOTLIN PSI SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, integer-literals, long-integer-literals -> paragraph 1 -> sentence 1
 * NUMBER: 3
 * DESCRIPTION: Hexadecimal integer literals suffixed by the long literal mark.
 */

val value = 0x1234567890L
val value = 0X23456789L
val value = 0x345678L
val value = 0X4567L
val value = 0X56L
val value = 0x65L
val value = 0X7654L
val value = 0x876543L
val value = 0x98765432L

val value = 0X0L
val value = 0x1L

val value = 0x100000L
val value = 0X1000001L

val value = 0X0000000L
val value = 0x0000001000000L
val value = 0x00000010L

val value = 0xABCDEFL
val value = 0XabcdefL
val value = 0xAbcDL
val value = 0XaL
val value = 0xEL
val value = 0xEeEeEeEeL
val value = 0XAAAAAAAAL
val value = 0xcDfL
val value = 0xAcccccccccAL

val value = 0x0123456789ABCDEFL
val value = 0x0123456789abcdefL
val value = 0XAA00AAL
val value = 0xBc12eFL
val value = 0xa0L
val value = 0XE1L
val value = 0xE1eE2eE3eE4eL
val value = 0XAAAAAAAA000000000L
val value = 0xcDf091L
val value = 0xAcccc0000cccccAL
val value = 0X0000000AL
val value = 0xe0000000L
val value = 0x0000000D0000000L
val value = 0xA0AL

val value = 0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFL
