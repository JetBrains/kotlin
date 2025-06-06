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

fun testHoldsInsAreIndependent1(foo: String?) {
    runIfNot(foo == null) {
        foo.length
    }
    runIf(foo == null) {
        foo<!UNSAFE_CALL!>.<!>length // expected: red. actual: green
    }
}

fun testHoldsInsAreIndependent2(foo: String?) {
    runIf(foo == null) {
        foo<!UNSAFE_CALL!>.<!>length // expected: red. actual: green
    }
    runIfNot(foo == null) {
        foo.length
    }
}

