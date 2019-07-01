/*
 * KOTLIN PSI SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, integer-literals, long-integer-literals -> paragraph 1 -> sentence 1
 * NUMBER: 10
 * DESCRIPTION: Long literal mark in not allowed places (it's considered as identifiers).
 */

val value = l1234
val value = L0xA0Al
val value = _l
val value = _L
