// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +AllowContractsOnPropertyAccessors
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts

import kotlin.contracts.*
import kotlin.contracts.InvocationKind

val (() -> Unit).callOnce: Int
    get() {
        contract { callsInPlace(this@callOnce, InvocationKind.EXACTLY_ONCE) }
        return 1
    }

val (() -> Unit).callAtMost: Int
    get() {
        contract { callsInPlace(this@callAtMost, InvocationKind.AT_MOST_ONCE) }
        return 1
    }

val (() -> Unit).callUnknown: Int
    get() {
        contract { callsInPlace(this@callUnknown, InvocationKind.UNKNOWN) }
        return 1
    }

var Int.callOnceInSetter: () -> Int
    get() {
        return { this }
    }
    set(value) {
        contract { callsInPlace(value, InvocationKind.EXACTLY_ONCE) }
        value()
    }

fun testSimple() {
    val b: Int
    {
        <!CAPTURED_VAL_INITIALIZATION!>b<!> = 1
    }.callAtMost

    val c: Int
    {
        <!CAPTURED_VAL_INITIALIZATION!>c<!> = 1
    }.callUnknown

    val d: Int
    {
        <!CAPTURED_VAL_INITIALIZATION!>d<!> = 1
    }.callOnce

    val e: Int
    1.callOnceInSetter = { <!CAPTURED_VAL_INITIALIZATION!>e<!> = 0 ; e }
}

var (() -> Unit)?.callExactlyOnce: Unit
    get() = Unit
    set(value) {
        contract {
            returns() implies (this@callExactlyOnce != null)
            <!ERROR_IN_CONTRACT_DESCRIPTION!>callsInPlace(this@callExactlyOnce!!, InvocationKind.EXACTLY_ONCE)<!>
        }
        this!!.invoke()
    }

fun testSetterSmartCast(f: (() -> Unit)?) {
    f.callExactlyOnce = Unit
    f()
}

/* GENERATED_FIR_TAGS: assignment, checkNotNullCall, contractCallsEffect, contractConditionalEffect, contracts,
equalityExpression, functionDeclaration, functionalType, getter, integerLiteral, lambdaLiteral, localProperty,
nullableType, propertyDeclaration, propertyWithExtensionReceiver, setter, smartcast, thisExpression */
