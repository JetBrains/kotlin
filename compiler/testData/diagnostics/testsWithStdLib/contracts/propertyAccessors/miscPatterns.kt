// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +AllowContractsOnPropertyAccessors
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts

import kotlin.contracts.*


val String?.pcTruth: Boolean
    get() {
        contract { returns(true) implies (this@pcTruth !is String) }
        return this !is String
    }

fun testPcTruthful(x: String?) {
    if (x.pcTruth) {
        x<!UNSAFE_CALL!>.<!>length
    } else {
        x<!UNSAFE_CALL!>.<!>length
    }
}

val String?.pcLiar: Boolean
    get() {
        contract { returns(true) implies (this@pcLiar !is String) }
        return false
    }

fun testPcLiar(x: String?) {
    if (x.pcLiar) {
        x<!UNSAFE_CALL!>.<!>length
    } else {
        x<!UNSAFE_CALL!>.<!>length
    }
}

val String?.returnsNullIfNull: Boolean?
    get() {
        contract { returns(null) implies (this@returnsNullIfNull !is String) }
        return if (this !is String) null else true
    }

fun testReturnsNull(x: String?) {
    if (x.returnsNullIfNull == null) {
        x<!UNSAFE_CALL!>.<!>length
    } else {
        x<!UNSAFE_CALL!>.<!>length
    }
}

val (() -> Unit)?.isPresent: Boolean
    get() {
        contract { returns(true) implies (this@isPresent is Function0<*>) }
        return this is Function0<*>
    }

fun testFunctionReceiver(f: (() -> Unit)?) {
    if (f.isPresent) {
        f()
    } else {
        <!UNSAFE_IMPLICIT_INVOKE_CALL!>f<!>()
    }
}

val String?.nullReturnPartial: Boolean?
    get() {
        contract { returns(null) implies (this@nullReturnPartial !is String) }
        return null
    }

fun testNullReturnPartial(s: String?) {
    if (s.nullReturnPartial == null) {
        s<!UNSAFE_CALL!>.<!>length
    }
}

var String.lenPositive: Unit
    get() = Unit
    set(v) {
        contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returns() implies (this@lenPositive.length > 0)<!> }
    }

fun testLenPositive(s: String) {
    s.lenPositive = Unit
    s.length
}

/* GENERATED_FIR_TAGS: assignment, comparisonExpression, contractConditionalEffect, contracts, equalityExpression,
functionDeclaration, functionalType, getter, ifExpression, integerLiteral, isExpression, lambdaLiteral, nullableType,
propertyDeclaration, propertyWithExtensionReceiver, setter, smartcast, starProjection, thisExpression */
