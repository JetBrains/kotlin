// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +AllowContractsOnPropertyAccessors
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts

import kotlin.contracts.*

val <T> T?.isNullCorrect: Boolean
    get() {
        contract {
            returns(true)  implies (this@isNullCorrect !is Any)
            returns(false) implies (this@isNullCorrect is Any)
        }
        return this !is Any
    }

fun testFullCorrect(x: String?) {
    if (x.isNullCorrect) {
        x<!UNSAFE_CALL!>.<!>length
    } else {
        x.length
    }
}

val String?.nullReturnFull: Boolean?
    get() {
        contract {
            returns(null) implies (this@nullReturnFull !is String)
            returnsNotNull() implies (this@nullReturnFull is String)
        }
        return if (this !is String) null else true
    }

fun testNullReturnFull(s: String?) {
    val r = s.nullReturnFull
    if (r == null) {
        s<!UNSAFE_CALL!>.<!>length
    } else {
        s.length
    }
}

val <T> T?.fullLiar: Boolean
    get() {
        contract {
            returns(true)  implies (this@fullLiar !is Any)
            returns(false) implies (this@fullLiar is Any)
        }
        return false
    }

fun testFullLiar(x: String?) {
    if (x.fullLiar) {
        x?.length
    } else {
        x.length
    }
}

val Any?.isStringFull: Boolean
    get() {
        contract {
            returns(true)  implies (this@isStringFull is String)
            returns(false) implies (this@isStringFull !is String)
        }
        return this is String
    }

fun testIsString(x: Any?) {
    if (x.isStringFull) {
        x.length
    } else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

val Boolean?.isReallyTrue: Boolean
    get() {
        contract {
            <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(true)  implies (this@isReallyTrue is Boolean && this@isReallyTrue)<!>
            <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(false) implies (this@isReallyTrue !is Boolean || !this@isReallyTrue)<!>
        }
        return this is Boolean && this
    }

fun testBoolReceiver(b: Boolean?) {
    if (b.isReallyTrue) {
        b<!UNSAFE_CALL!>.<!>not()
    } else {
        b<!UNSAFE_CALL!>.<!>not()
    }
}

@Suppress("USELESS_IS_CHECK")
var Boolean?.boolProp: Boolean
    get() {
        contract {
            <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(true)  implies (this@boolProp is Boolean && this@boolProp)<!>
            <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(false) implies (this@boolProp !is Boolean || !this@boolProp)<!>
        }
        return this is Boolean && this
    }
    set(v) {
        contract { returns() implies (v is Boolean && v) }
    }

fun testBoolProp(b: Boolean?) {
    if (b.boolProp) {
        b<!UNSAFE_CALL!>.<!>not()
    }
    b.boolProp = true
}

/* GENERATED_FIR_TAGS: andExpression, assignment, contractConditionalEffect, contracts, disjunctionExpression,
equalityExpression, functionDeclaration, getter, ifExpression, isExpression, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, propertyWithExtensionReceiver, safeCall, setter, smartcast, thisExpression, typeParameter */
