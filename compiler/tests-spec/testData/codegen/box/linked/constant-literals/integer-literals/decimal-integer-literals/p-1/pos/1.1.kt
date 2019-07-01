/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, integer-literals, decimal-integer-literals -> paragraph 1 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: Sequences with decimal digit symbols.
 */

val value_1 = 1234567890
val value_2 = 23456789
val value_3 = 345678
val value_4 = 4567
val value_5 = 56
val value_6 = 65
val value_7 = 7654
val value_8 = 876543
val value_9 = 98765432

fun box(): String? {
    val value_10 = 0
    val value_11 = 1
    val value_12 = 100000
    val value_13 = 1000001

    if (value_1 != 1234567890) return null
    if (value_2 != 23456789) return null
    if (value_3 != 345678) return null
    if (value_4 != 4567) return null
    if (value_5 != 56) return null
    if (value_6 != 65) return null
    if (value_7 != 7654) return null
    if (value_8 != 876543) return null
    if (value_9 != 98765432) return null
    if (value_10 != 0) return null
    if (value_11 != 1) return null
    if (value_12 != 100000) return null
    if (value_13 != 1000001) return null

    return "OK"
}
