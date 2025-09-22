// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +AllowContractsOnPropertyAccessors
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts

import kotlin.contracts.*

class Host {
    val String?.pcTruthM: Boolean
        get() {
            contract { returns(true) implies (this@pcTruthM == null) }
            return this == null
        }

    fun testPcTruthful_member(x: String?) {
        if (x.pcTruthM) {
            x<!UNSAFE_CALL!>.<!>length
        } else {
            x<!UNSAFE_CALL!>.<!>length
        }
    }

    val String?.pcLiarM: Boolean
        get() {
            contract { returns(true) implies (this@pcLiarM == null) }
            return false
        }


    fun testPcLiar_member(x: String?) {
        if (x.pcLiarM) {
            x<!UNSAFE_CALL!>.<!>length
        } else {
            x<!UNSAFE_CALL!>.<!>length
        }
    }

    val String?.returnsNullIfNullM: Boolean?
        get() {
            contract { returns(null) implies (this@returnsNullIfNullM == null) }
            return if (this == null) null else true
        }

    fun testReturnsNull_member(x: String?) {
        if (x.returnsNullIfNullM == null) {
            x<!UNSAFE_CALL!>.<!>length
        } else {
            x<!UNSAFE_CALL!>.<!>length
        }
    }

    val (() -> Unit)?.isPresentM: Boolean
        get() {
            contract { returns(true) implies (this@isPresentM != null) }
            return this != null
        }

    fun testFunctionReceiver_member(f: (() -> Unit)?) {
        if (f.isPresentM) {
            f()
        } else {
            <!UNSAFE_IMPLICIT_INVOKE_CALL!>f<!>()
        }
    }

    var String.lenPositiveM: Unit
        get() = Unit
        set(v) {
            contract {
                <!ERROR_IN_CONTRACT_DESCRIPTION!>returns() implies (this@lenPositiveM.length > 0)<!>
            }
        }

    fun testLenPositive_member(s: String) {
        s.lenPositiveM = Unit
        s.length
    }
}

fun pcTruth_top(x: String?): Boolean {
    contract { returns(true) implies (x == null) }
    return x == null
}

fun testPcTruthful_top(x: String?) {
    if (pcTruth_top(x)) {
        x<!UNSAFE_CALL!>.<!>length
    } else {
        x<!UNSAFE_CALL!>.<!>length
    }
}

fun pcLiar_top(x: String?): Boolean {
    contract { returns(true) implies (x == null) }
    return false
}

fun testPcLiar_top(x: String?) {
    if (pcLiar_top(x)) {
        x<!UNSAFE_CALL!>.<!>length
    } else {
        x<!UNSAFE_CALL!>.<!>length
    }
}

fun returnsNullIfNull_top(x: String?): Boolean? {
    contract { returns(null) implies (x == null) }
    return if (x == null) null else true
}

fun testReturnsNull_top(x: String?) {
    if (returnsNullIfNull_top(x) == null) {
        x<!UNSAFE_CALL!>.<!>length
    } else {
        x<!UNSAFE_CALL!>.<!>length
    }
}

fun isPresent_top(f: (() -> Unit)?): Boolean {
    contract { returns(true) implies (f != null) }
    return f != null
}

fun testFunctionReceiver_top(f: (() -> Unit)?) {
    if (isPresent_top(f)) {
        f()
    } else {
        <!UNSAFE_IMPLICIT_INVOKE_CALL!>f<!>()
    }
}

fun setLenPositive_top(s: String, v: Unit) {
    contract {
        <!ERROR_IN_CONTRACT_DESCRIPTION!>returns() implies (s.length > 0)<!>
    }
}

fun testLenPositive_top(s: String) {
    setLenPositive_top(s, Unit)
    s.length
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, comparisonExpression, contractConditionalEffect, contracts,
equalityExpression, functionDeclaration, functionalType, getter, ifExpression, integerLiteral, lambdaLiteral,
nullableType, propertyDeclaration, propertyWithExtensionReceiver, setter, smartcast, thisExpression */
