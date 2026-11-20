// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

class ExactlyOnce {
    constructor(block: () -> Unit) {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        block()
    }
}

class AtMostOnce {
    constructor(condition: Boolean, block: () -> Unit) {
        contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }
        if (condition) block()
    }
}

class Generic<T> {
    constructor(block: () -> T) {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        block()
    }
}

typealias ExactlyOnceAlias = ExactlyOnce
typealias AtMostOnceAlias = AtMostOnce
typealias GenericAlias = Generic<Int>

fun exactlyOnceDirect() {
    val x: Int
    ExactlyOnce { x = 1 }
    x.inc()
}

fun exactlyOnceViaAlias() {
    val x: Int
    ExactlyOnceAlias { x = 1 }
    x.inc()
}

fun atMostOnceDirect() {
    val x: Int
    AtMostOnce(true) { x = 1 }
    <!UNINITIALIZED_VARIABLE!>x<!>.inc()
}

fun atMostOnceViaAlias() {
    val x: Int
    AtMostOnceAlias(true) { x = 1 }
    <!UNINITIALIZED_VARIABLE!>x<!>.inc()
}

fun genericDirect() {
    val x: Int
    Generic { x = 1; 2 }
    x.inc()
}

fun genericViaAlias() {
    val x: Int
    GenericAlias { x = 1; 2 }
    x.inc()
}

fun inPlaceLambdaDirect(block: () -> Unit) {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    ExactlyOnce(block)
}

fun inPlaceLambdaViaAlias(block: () -> Unit) {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    ExactlyOnceAlias(block)
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, contractCallsEffect, contracts, functionDeclaration, functionalType,
ifExpression, integerLiteral, lambdaLiteral, localProperty, nullableType, propertyDeclaration, secondaryConstructor,
typeAliasDeclaration, typeParameter */
