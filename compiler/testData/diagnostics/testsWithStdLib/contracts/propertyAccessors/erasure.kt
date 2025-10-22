// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +AllowContractsOnPropertyAccessors
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts

import kotlin.contracts.*
import kotlin.contracts.InvocationKind

val <T> T?.erasedCheck: Boolean
    get() {
        contract {
            returns() implies (this@erasedCheck is List<T>)
        }
        return true
    }

fun useErasedCheck(x: Any?) {
    if (x.erasedCheck) {
        x.size
    }
}

val <T> T?.erasedCheck1: Unit
    get() {
        contract {
            returns() implies (this@erasedCheck1 is List<*>)
        }
    }

fun useErasedCheck1(x: Any?) {
    x.erasedCheck1
    x.size
}

val <T> T?.illegalImplies: Boolean
    get() {
        contract {
            <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(true) implies (this@illegalImplies is T)<!>
        }
        return false
    }

fun useIllegalImplies(x: Any?) {
    if (x.illegalImplies) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

var <T> T?.illegalSetter: T?
    get() = this
    set(v) {
        contract {
            <!ERROR_IN_CONTRACT_DESCRIPTION!>returns() implies (v is T)<!>
        }
    }

fun useIllegalSetter(arg: String?) {
    "".illegalSetter = arg
    arg<!UNSAFE_CALL!>.<!>length
}

/* GENERATED_FIR_TAGS: assignment, contractCallsEffect, contractConditionalEffect, contracts, equalityExpression,
functionDeclaration, functionalType, getter, ifExpression, isExpression, lambdaLiteral, nullableType,
propertyDeclaration, propertyWithExtensionReceiver, safeCall, setter, smartcast, starProjection, stringLiteral,
thisExpression, typeParameter */
