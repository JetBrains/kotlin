// !OPT_IN: kotlin.contracts.ExperimentalContracts
// WITH_STDLIB
// ISSUE: KT-53308

import kotlin.contracts.*

fun <E> checkEquality(arg: E?) { contract { returns() implies (arg != null) } }
fun <E> checkIdentity(arg: E?) { contract { returns() implies (arg !== null) } }
fun checkTrue(statement: Boolean) { contract { returns() implies statement } }
fun checkFalse(statement: Boolean) { contract { returns() implies !statement } }

fun consume(arg: String) {}

fun test(a: String?) {
    a.let {
        checkEquality(it)
        consume(it)
    }
    a.let {
        checkIdentity(it)
        consume(it)
    }
    a.let {
        checkTrue(it != null)
        consume(it)
    }
    a.let {
        checkTrue(it !== null)
        consume(it)
    }
    a.let {
        checkFalse(it == null)
        consume(it)
    }
    a.let {
        checkFalse(it === null)
        consume(it)
    }
}
