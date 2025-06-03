// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +HoldsInContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts

import kotlin.contracts.*

inline fun <R> runIf(condition: Boolean, block: () -> R): R? {
    contract { condition holdsIn block }
    return if (condition) { block() } else null
}

fun testRunIf(s: Any) {
    val x = runIf(s is String) {
        s.length
        Unit
    }
}

fun testRunIfErr(s: Any) {
    val y = runIf(s is Int) {
        s.<!UNRESOLVED_REFERENCE!>length<!>
        Unit
    }
}

inline fun <R> runIfNot(condition: Boolean, block: () -> R): R? {
    contract {
        !condition holdsIn block
    }
    return if (!condition) block() else null
}

fun testRunIfNot(s: String?) {
    runIfNot(s == null) {
        s.length
    }
}

fun testRunIfNotErr(s: String?) {
    runIfNot(s !== null) {
        s<!UNSAFE_CALL!>.<!>length
    }
}

fun main() {
    var foo: String? = ""
    runIfNot(foo == null) {
        foo.length // expected: green. actual: red
    }
}

@OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)
inline fun <R> foo(condition: Boolean, block: () -> R): R? {
    contract { false holdsIn block } // doesn't make sense (unless `block` is unused). The same for `true`
    return if (condition) { block() } else null
}

