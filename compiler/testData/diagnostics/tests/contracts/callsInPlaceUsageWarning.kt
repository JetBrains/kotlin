// RUN_PIPELINE_TILL: BACKEND
// OPT_IN: kotlin.contracts.ExperimentalContracts
// DIAGNOSTICS: -UNUSED_VARIABLE

import kotlin.contracts.*

fun maybeString(): String? = "ok"

// Helper function with callsInPlace contract
fun testCallsInPlace(block: () -> Int) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
}

fun testCallsInPlaceAtMostOnce(block: () -> Int) {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (Math.random() > 0.5) {
        block()
    }
}

fun testCallsInPlaceAtLeastOnce(block: () -> Int) {
    contract {
        callsInPlace(block, InvocationKind.AT_LEAST_ONCE)
    }
    block()
}

// TESTCASE NUMBER: 1
// Common use case: callsInPlace ensures smart cast is safe in lambda
fun useCaseExactlyOnce() {
    var x: String? = "hello"
    if (x != null) {
        testCallsInPlace {
            // x is smartcasted to String because of callsInPlace contract
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
        }
    }
    x = null // Without callsInPlace, this mutation would make smart cast unsafe
}

// TESTCASE NUMBER: 2
// Smart cast with AT_MOST_ONCE
fun useCaseAtMostOnce() {
    var x: String? = "hello"
    if (x != null) {
        testCallsInPlaceAtMostOnce {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
        }
    }
    x = null
}

// TESTCASE NUMBER: 3
// Smart cast with AT_LEAST_ONCE
fun useCaseAtLeastOnce() {
    var x: String? = "hello"
    if (x != null) {
        testCallsInPlaceAtLeastOnce {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
        }
    }
    x = null
}

// TESTCASE NUMBER: 4
// Multiple smart casts in the same lambda
fun useCaseMultipleSmartCasts() {
    var x: String? = "hello"
    var y: Int? = 42
    if (x != null && y != null) {
        testCallsInPlace {
            <!SMARTCAST_IMPOSSIBLE!>y<!> + 1 + <!SMARTCAST_IMPOSSIBLE!>x<!>.length
        }
    }
    x = null
    y = null
}

// TESTCASE NUMBER: 5
// Nested lambdas with smart casts
fun useCaseNestedLambdas() {
    var x: String? = "hello"
    var y: Int? = 42
    if (x != null && y != null) {
        testCallsInPlace {
            testCallsInPlace {
                <!SMARTCAST_IMPOSSIBLE!>x<!>.length
            }
            <!SMARTCAST_IMPOSSIBLE, SMARTCAST_IMPOSSIBLE!>y<!>
        }
    }
    y = null
    x = null
}

fun regularFunction(block: () -> Int) {
    block()
}

inline fun inlineFunction(block: () -> Int) {
    block()
}

fun useCaseWithoutContractWithStable() {
    var x: String? = "hello"
    if (x != null) {
        regularFunction {
            <!DEBUG_INFO_SMARTCAST!>x<!>.length
        }
        inlineFunction {
            <!DEBUG_INFO_SMARTCAST!>x<!>.length
        }
    }
}

fun nestedSmartcastInOtherInPlaceLambda() {
    var x: String? = maybeString()
    testCallsInPlaceAtMostOnce {
        if (x is String) {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
        }
        testCallsInPlaceAtMostOnce {
            x = null
            2
        }
        2
    }
}

fun sameOwnerWrite() {
    var x: String? = maybeString()
    testCallsInPlace {
        if (x != null) {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
        }
        x = null
        2
    }
}

/* GENERATED_FIR_TAGS: additiveExpression, andExpression, assignment, comparisonExpression, contractCallsEffect,
contracts, equalityExpression, functionDeclaration, functionalType, ifExpression, inline, integerLiteral, javaFunction,
lambdaLiteral, localProperty, nullableType, propertyDeclaration, smartcast, stringLiteral */
