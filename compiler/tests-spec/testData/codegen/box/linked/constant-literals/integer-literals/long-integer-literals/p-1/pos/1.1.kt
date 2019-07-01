/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, integer-literals, long-integer-literals -> paragraph 1 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: Decimal integer literals with long literal mark.
 */

val value_1 = 0L

val value_2 = 127L
val value_3 = 128L
val value_4 = -128L
val value_5 = -129L

val value_6 = 32767L
val value_7 = 32768L
val value_8 = -32768L
val value_9 = -32769L

val value_10 = 2147483647L
val value_11 = 2147483648L
val value_12 = -2147483648L
val value_13 = -2147483649L

fun box(): String? {
    val value_14 = 9223372036854775807L
    val value_15 = -9223372036854775807L

    if (value_1 != 0L) return null
    if (value_2 != 127L) return null
    if (value_3 != 128L) return null
    if (value_4 != -128L) return null
    if (value_5 != -129L) return null
    if (value_6 != 32767L) return null
    if (value_7 != 32768L) return null
    if (value_8 != -32768L) return null
    if (value_9 != -32769L) return null
    if (value_10 != 2147483647L) return null
    if (value_11 != 2147483648L) return null
    if (value_12 != -2147483648L) return null
    if (value_13 != -2147483649L) return null
    if (value_14 != 9223372036854775807L) return null
    if (value_15 != -9223372036854775807L) return null

    return "OK"
}
