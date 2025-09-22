// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +AllowContractsOnPropertyAccessors
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts

import kotlin.contracts.*

inline val <T> T?.isNotNull: Boolean
    get() {
        contract { returns(true) implies (this@isNotNull != null) }
        return this != null
    }

fun processString(str: String?) {
    if (str.isNotNull) {
        str.length
    } else {
        str<!UNSAFE_CALL!>.<!>length
    }
}

val String?.notNullInlineGetter: Boolean
    inline get() {
        contract { returns(true) implies (this@notNullInlineGetter != null) }
        return this != null
    }

fun testInlineGetter(x: String?) {
    if (x.notNullInlineGetter) {
        x.length
    } else {
        x<!UNSAFE_CALL!>.<!>length
    }
}

var Int?.a : String
    get() = ""
    inline set(value) {
        contract {
            returns() implies (this@a != null)
        }
    }

/* GENERATED_FIR_TAGS: contractConditionalEffect, contracts, equalityExpression, functionDeclaration, getter,
ifExpression, lambdaLiteral, nullableType, propertyDeclaration, propertyWithExtensionReceiver, setter, smartcast,
stringLiteral, thisExpression, typeParameter */
