// RUN_PIPELINE_TILL: BACKEND
// OPT_IN: kotlin.contracts.ExperimentalContracts
// LANGUAGE: +AllowContractsOnPropertyAccessors

import kotlin.contracts.*

var Int?.prop : Int?
    get() {
        contract { returns() implies (this@prop != null) }
        return null
    }
    set(v: Int?) {
        contract {
            returns() implies (v != null)
            returns() implies (this@prop != null)
        }
    }

fun test1(v: Int?) {
    val vv = v.prop
    v + 1
}

fun test2(v: Int?, newv: Int) {
    v.prop = newv
    v + 1
}

fun test3(v: Int, newv: Int?) {
    v.prop = newv
    newv + 1
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, contractConditionalEffect, contracts, equalityExpression,
functionDeclaration, getter, integerLiteral, lambdaLiteral, localProperty, nullableType, propertyDeclaration,
propertyWithExtensionReceiver, setter, smartcast, thisExpression */
