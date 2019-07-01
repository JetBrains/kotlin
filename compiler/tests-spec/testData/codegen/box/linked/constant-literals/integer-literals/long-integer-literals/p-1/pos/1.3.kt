/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, integer-literals, long-integer-literals -> paragraph 1 -> sentence 1
 * NUMBER: 3
 * DESCRIPTION: Binary integer literals with long literal mark.
 */

val value_1 = 0b0L

val value_2 = 0B1111111L
val value_3 = 0b10000000L
val value_4 = -0b10000000L
val value_5 = -0b10000001L

val value_6 = 0B111111111111111L
val value_7 = 0b1000000000000000L
val value_8 = -0b1000000000000000L
val value_9 = -0b1000000000000001L

val value_10 = 0b1111111111111111111111111111111L
val value_11 = 0B10000000000000000000000000000000L
val value_12 = -0B10000000000000000000000000000000L
val value_13 = -0b10000000000000000000000000000001L

fun box(): String? {
    val value_14 = 0X7FFFFFFFFFFFFFFFL
    val value_15 = -0X7FFFFFFFFFFFFFFFL

    if (value_1 != 0b0L) return null
    if (value_2 != 0B1111111L) return null
    if (value_3 != 0b10000000L) return null
    if (value_4 != -0b10000000L) return null
    if (value_5 != -0b10000001L) return null
    if (value_6 != 0B111111111111111L) return null
    if (value_7 != 0b1000000000000000L) return null
    if (value_8 != -0b1000000000000000L) return null
    if (value_9 != -0b1000000000000001L) return null
    if (value_10 != 0b1111111111111111111111111111111L) return null
    if (value_11 != 0B10000000000000000000000000000000L) return null
    if (value_12 != -0B10000000000000000000000000000000L) return null
    if (value_13 != -0b10000000000000000000000000000001L) return null
    if (value_14 != 0b111111111111111111111111111111111111111111111111111111111111111L) return null
    if (value_15 != -0B111111111111111111111111111111111111111111111111111111111111111L) return null

    return "OK"
}
