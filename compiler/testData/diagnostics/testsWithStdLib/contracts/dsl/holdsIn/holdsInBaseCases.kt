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
        foo<!UNSAFE_CALL!>.<!>length
    }
}

fun testHoldsInsAreIndependent2(foo: String?) {
    runIf(foo == null) {
        foo<!UNSAFE_CALL!>.<!>length
    }
    runIfNot(foo == null) {
        foo.length
    }
}

fun testRunIfNotWithLaterAssignment1() {
    var s: String? = null
    runIfNot(s == null) {
        s<!UNSAFE_CALL!>.<!>length
        s = null
    }
}

fun testRunIfNotWithLaterAssignment2() {
    var s: String? = null
    runIfNot(s == null) {
        s = ""
        s.length
    }
}

inline fun <R> runIfElse(condition: Boolean, ifTrue: () -> R, ifFalse: () -> R, unrelated: () -> Unit): R? {
    contract {
        condition holdsIn ifTrue
        !condition holdsIn ifFalse
        callsInPlace(ifTrue)
        callsInPlace(ifFalse)
        callsInPlace(unrelated)
    }
    unrelated()
    return if (condition) ifTrue() else ifFalse()
}

fun testRunIfElse1(foo: String?) {
    runIfElse(
        foo == null,
        ifTrue = {
            foo<!UNSAFE_CALL!>.<!>length
        },
        ifFalse = {
            foo.length
        },
        unrelated = {
            foo<!UNSAFE_CALL!>.<!>length
        }
    )
}

fun testRunIfElse2(foo: String?) {
    runIfElse(
        foo != null,
        ifTrue = {
            foo.length
        },
        ifFalse = {
            foo<!UNSAFE_CALL!>.<!>length
        },
        unrelated = {
            foo<!UNSAFE_CALL!>.<!>length
        }
    )
}

fun testRunIfElseWithUnrelatedAssignmentAfter(foo: String?) {
    var foo: String? = ""
    runIfElse(
        foo == null,
        ifTrue = {
            foo<!UNSAFE_CALL!>.<!>length
        },
        ifFalse = {
            <!SMARTCAST_IMPOSSIBLE!>foo<!>.length
        },
        unrelated = {
            foo = null
        }
    )
}

fun testRunIfElseWithUnrelatedAssignmentBefore(foo: String?) {
    var foo: String? = ""
    runIfElse(
        foo == null,
        unrelated = {
            foo = null
        },
        ifTrue = {
            foo<!UNSAFE_CALL!>.<!>length
        },
        ifFalse = {
            <!SMARTCAST_IMPOSSIBLE!>foo<!>.length
        }
    )
}

/* GENERATED_FIR_TAGS: assignment, contractCallsEffect, contractHoldsInEffect, contracts, equalityExpression,
functionDeclaration, functionalType, ifExpression, inline, isExpression, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, smartcast, stringLiteral, typeParameter */
