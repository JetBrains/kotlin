/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, integer-literals, long-integer-literals -> paragraph 1 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: Hexadecimal integer literals with long literal mark.
 */

val value_1 = 0x0L

val value_2 = 0x7FL
val value_3 = 0X80L
val value_4 = -0X80L
val value_5 = -0x81L

val value_6 = 0x7FFFL
val value_7 = 0x8000L
val value_8 = -0x8000L
val value_9 = -0x8001L

val value_10 = 0x7FFFFFFFL
val value_11 = 0x80000000L
val value_12 = -0x80000000L
val value_13 = -0x80000001L

fun box(): String? {
    val value_14 = 0X7FFFFFFFFFFFFFFFL
    val value_15 = -0X7FFFFFFFFFFFFFFFL

    if (value_1 != 0x0L) return null
    if (value_2 != 0x7FL) return null
    if (value_3 != 0X80L) return null
    if (value_4 != -0X80L) return null
    if (value_5 != -0x81L) return null
    if (value_6 != 0x7FFFL) return null
    if (value_7 != 0x8000L) return null
    if (value_8 != -0x8000L) return null
    if (value_9 != -0x8001L) return null
    if (value_10 != 0x7FFFFFFFL) return null
    if (value_11 != 0x80000000L) return null
    if (value_12 != -0x80000000L) return null
    if (value_13 != -0x80000001L) return null
    if (value_14 != 0X7FFFFFFFFFFFFFFFL) return null
    if (value_15 != -0X7FFFFFFFFFFFFFFFL) return null

    return "OK"
}
