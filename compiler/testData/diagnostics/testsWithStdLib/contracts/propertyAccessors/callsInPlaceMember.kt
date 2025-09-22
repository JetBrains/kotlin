// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +AllowContractsOnPropertyAccessors
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts

import kotlin.contracts.*
import kotlin.contracts.InvocationKind

class Host {
    val (() -> Unit).callOnceM: Int
        get() {
            contract { callsInPlace(this@callOnceM, InvocationKind.EXACTLY_ONCE) }
            return 1
        }

    val (() -> Unit).callAtMostM: Int
        get() {
            contract { callsInPlace(this@callAtMostM, InvocationKind.AT_MOST_ONCE) }
            return 1
        }

    val (() -> Unit).callUnknownM: Int
        get() {
            contract { callsInPlace(this@callUnknownM, InvocationKind.UNKNOWN) }
            return 1
        }

    var Int.callOnceInSetterM: () -> Int
        get() {
            return { this }
        }
        set(value) {
            contract { callsInPlace(value, InvocationKind.EXACTLY_ONCE) }
            value()
        }

    var (() -> Unit)?.callExactlyOnceM: Unit
        get() = Unit
        set(value) {
            contract {
                returns() implies (this@callExactlyOnceM != null)
                <!ERROR_IN_CONTRACT_DESCRIPTION!>callsInPlace(this@callExactlyOnceM!!, InvocationKind.EXACTLY_ONCE)<!>
            }
            this!!.invoke()
        }
}

fun testSimpleMember() {
    val h = Host()
    with(h) {
        val b: Int
        {
            <!CAPTURED_VAL_INITIALIZATION!>b<!> = 1
        }.callAtMostM

        val c: Int
        {
            <!CAPTURED_VAL_INITIALIZATION!>c<!> = 1
        }.callUnknownM

        val d: Int
        {
            <!CAPTURED_VAL_INITIALIZATION!>d<!> = 1
        }.callOnceM

        val e: Int
        1.callOnceInSetterM = { <!CAPTURED_VAL_INITIALIZATION!>e<!> = 0 ; e }
    }
}

fun testSetterSmartCastMember(f: (() -> Unit)?) {
    val h = Host()
    with(h) {
        f.callExactlyOnceM = Unit
        f()
    }
}


var callOnceInSetterTop: () -> Int
    get() = { 0 }
    set(value) {
        contract { callsInPlace(value, InvocationKind.EXACTLY_ONCE) }
        value()
    }

var callExactlyOnceTop: (() -> Unit)?
    get() = null
    set(value) {
        contract {
            returns() implies (value != null)
            <!ERROR_IN_CONTRACT_DESCRIPTION!>callsInPlace(value!!, InvocationKind.EXACTLY_ONCE)<!>
        }
        value!!.invoke()
    }

fun testSimpleTop() {
    val e: Int
    callOnceInSetterTop = { <!CAPTURED_VAL_INITIALIZATION!>e<!> = 0 ; e }
}

fun testSetterSmartCast_top(f: (() -> Unit)?) {
    callExactlyOnceTop = f
    f()
}

/* GENERATED_FIR_TAGS: assignment, checkNotNullCall, classDeclaration, contractCallsEffect, contractConditionalEffect,
contracts, equalityExpression, functionDeclaration, functionalType, getter, integerLiteral, lambdaLiteral, localProperty,
nullableType, propertyDeclaration, propertyWithExtensionReceiver, setter, smartcast, thisExpression */
