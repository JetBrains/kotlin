// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-90659
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

interface I {
    fun run(block: () -> Int): Int {
        <!CONTRACT_NOT_ALLOWED!>contract<!> { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        return block()
    }

    fun isString(x: Any?): Boolean {
        <!CONTRACT_NOT_ALLOWED!>contract<!> { returns(true) implies (x is String) }
        return x is String
    }
}

interface G<T> {
    fun run(block: () -> T): T {
        <!CONTRACT_NOT_ALLOWED!>contract<!> { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        return block()
    }

    fun isString(x: Any?): Boolean {
        <!CONTRACT_NOT_ALLOWED!>contract<!> { returns(true) implies (x is String) }
        return x is String
    }
}

class Delegated(i: I) : I by i

class DelegatedGeneric(g: G<Int>) : G<Int> by g

open class OpenDelegated<T>(g: G<T>) : G<T> by g

class SubOfDelegated(g: G<Int>) : OpenDelegated<Int>(g)

fun assignmentViaInterface(i: I) {
    val x: Int
    i.run { x = 1; 2 }
    x.inc()
}

// Delegated members don't take contracts of the interface members
fun assignmentViaDelegated(d: Delegated) {
    val x: Int
    d.run { <!CAPTURED_VAL_INITIALIZATION!>x<!> = 1; 2 }
    <!UNINITIALIZED_VARIABLE!>x<!>.inc()
}

fun assignmentViaDelegatedGeneric(d: DelegatedGeneric) {
    val x: Int
    d.run { <!CAPTURED_VAL_INITIALIZATION!>x<!> = 1; 2 }
    <!UNINITIALIZED_VARIABLE!>x<!>.inc()
}

fun assignmentViaSubOfDelegated(d: SubOfDelegated) {
    val x: Int
    d.run { <!CAPTURED_VAL_INITIALIZATION!>x<!> = 1; 2 }
    <!UNINITIALIZED_VARIABLE!>x<!>.inc()
}

fun inPlaceLambdaViaInterface(i: I, block: () -> Int) {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    i.run(block)
}

fun inPlaceLambdaViaDelegated(d: Delegated, block: () -> Int) {
    contract { <!LEAKED_IN_PLACE_LAMBDA, WRONG_INVOCATION_KIND!>callsInPlace(block, InvocationKind.EXACTLY_ONCE)<!> }
    d.run(<!LEAKED_IN_PLACE_LAMBDA!>block<!>)
}

fun inPlaceLambdaViaDelegatedGeneric(d: DelegatedGeneric, block: () -> Int) {
    contract { <!LEAKED_IN_PLACE_LAMBDA, WRONG_INVOCATION_KIND!>callsInPlace(block, InvocationKind.EXACTLY_ONCE)<!> }
    d.run(<!LEAKED_IN_PLACE_LAMBDA!>block<!>)
}

fun inPlaceLambdaViaSubOfDelegated(d: SubOfDelegated, block: () -> Int) {
    contract { <!LEAKED_IN_PLACE_LAMBDA, WRONG_INVOCATION_KIND!>callsInPlace(block, InvocationKind.EXACTLY_ONCE)<!> }
    d.run(<!LEAKED_IN_PLACE_LAMBDA!>block<!>)
}

fun smartcastViaInterface(i: I, a: Any?): Int = if (i.isString(a)) a.length else 0

fun smartcastViaDelegated(d: Delegated, a: Any?): Int = if (d.isString(a)) a.<!UNRESOLVED_REFERENCE!>length<!> else 0

fun smartcastViaDelegatedGeneric(d: DelegatedGeneric, a: Any?): Int = if (d.isString(a)) a.<!UNRESOLVED_REFERENCE!>length<!> else 0

fun smartcastViaSubOfDelegated(d: SubOfDelegated, a: Any?): Int = if (d.isString(a)) a.<!UNRESOLVED_REFERENCE!>length<!> else 0

/* GENERATED_FIR_TAGS: assignment, classDeclaration, contractCallsEffect, contractConditionalEffect, contracts,
functionDeclaration, functionalType, ifExpression, inheritanceDelegation, integerLiteral, interfaceDeclaration,
isExpression, lambdaLiteral, localProperty, nullableType, primaryConstructor, propertyDeclaration, smartcast,
typeParameter */
