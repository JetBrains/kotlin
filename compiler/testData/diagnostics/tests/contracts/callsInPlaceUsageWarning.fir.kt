// RUN_PIPELINE_TILL: BACKEND
// OPT_IN: kotlin.contracts.ExperimentalContracts
// DIAGNOSTICS: -UNUSED_VARIABLE

import kotlin.contracts.*

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
            <!SMARTCAST_RELYING_ON_CALLS_IN_PLACE!>x<!>.length
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
            <!SMARTCAST_RELYING_ON_CALLS_IN_PLACE!>x<!>.length
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
            <!SMARTCAST_RELYING_ON_CALLS_IN_PLACE!>x<!>.length
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
            <!SMARTCAST_RELYING_ON_CALLS_IN_PLACE!>y<!> + 1 + <!SMARTCAST_RELYING_ON_CALLS_IN_PLACE!>x<!>.length
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
                <!SMARTCAST_RELYING_ON_CALLS_IN_PLACE!>x<!>.length
            }
            <!SMARTCAST_RELYING_ON_CALLS_IN_PLACE!>y<!>
        }
    }
    y = null
    x = null
}

fun regularFunction(block: () -> Int) {
    block()
}

fun useCaseWithoutContractWithStable() {
    var x: String? = "hello"
    if (x != null) {
        regularFunction {
            x.length
        }
    }
}

/* GENERATED_FIR_TAGS: additiveExpression, andExpression, assignment, comparisonExpression, contractCallsEffect,
contracts, equalityExpression, functionDeclaration, functionalType, ifExpression, inline, integerLiteral, javaFunction,
lambdaLiteral, localProperty, nullableType, propertyDeclaration, smartcast, stringLiteral */
