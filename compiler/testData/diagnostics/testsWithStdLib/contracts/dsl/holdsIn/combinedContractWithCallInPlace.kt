// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +HoldsInContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

inline fun <R> holdsInExactlyOnce(condition: Boolean, block: () -> R) {
    contract {
        condition holdsIn block
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
}

inline fun <R> holdsInAtLeastOnce(condition: Boolean, block: () -> R) {
    contract {
        condition holdsIn block
        callsInPlace(block, InvocationKind.AT_LEAST_ONCE)
    }
    block()
}

inline fun <R> holdsInWithTwoCallInPlace(condition: Boolean, block: () -> R, block2: ()-> R) {
    contract {
        condition holdsIn block
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        callsInPlace(block2, InvocationKind.AT_LEAST_ONCE)
    }
    block()
    block2()
}

fun <T> materialize(): T = null!!

fun test1() {
    var x: Any = materialize()
    var y: Any = materialize()
    holdsInExactlyOnce(x is String) {
        x.length
        x = 10
    }

    holdsInAtLeastOnce(y is String) {
        y.<!UNRESOLVED_REFERENCE!>length<!>
        y = 10
    }
}

fun test2() {
    var x: Any = materialize()
    var y: Any = materialize()
    holdsInExactlyOnce(x is String) {
        x.length
    }

    holdsInAtLeastOnce(y is String) {
        y.length
    }
    x = 10
    y = 10
}

fun test3() {
    var x: Any? = materialize()
    var y = x
    holdsInExactlyOnce(y is String) {
        y = 10
        y = x
        x.length
        y.length
    }
}

fun test4() {
    var x: Any? = materialize()
    var y = x
    holdsInAtLeastOnce(y is String) {
        y = 10
        y = x
        x.length
        y.length
    }
}

fun test5() {
    var x: Any = materialize()
    var y: Any = materialize()
    val state: Boolean = x is String
    val state2: Boolean = y is String
    holdsInExactlyOnce(state) {
        x.length
    }

    holdsInAtLeastOnce(state2) {
        y.length
    }
}

fun test6() {
    var x: Any? = materialize()

    holdsInWithTwoCallInPlace(
        (x is String),
        { <!SMARTCAST_IMPOSSIBLE!>x<!>.length },
        { x = 10 }
    )
}

/* GENERATED_FIR_TAGS: assignment, checkNotNullCall, contractCallsEffect, contractHoldsInEffect, contracts,
functionDeclaration, functionalType, inline, integerLiteral, isExpression, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, smartcast, typeParameter */
