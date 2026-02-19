// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// LANGUAGE: +AllowCheckForErasedTypesInContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

fun <T : Any> asListOfT(value: Any?): Boolean {
    contract { returns(true) implies (value is List<T>) }
    return value is List<*>
}

fun demoParam(x: Any?) {
    if (asListOfT<String>(x)) {
        x[0].length
    }
}

fun <T : Any> List<*>.assertListOfT(): Boolean {
    contract { returns(true) implies (this@assertListOfT is List<T>) }
    return true
}

fun demoReceiver(xs: List<*>) {
    if (xs.assertListOfT<String>()) {
        xs.first().length
    }
}

/* GENERATED_FIR_TAGS: contractConditionalEffect, contracts, funWithExtensionReceiver, functionDeclaration, ifExpression,
integerLiteral, isExpression, lambdaLiteral, nullableType, smartcast, starProjection, thisExpression, typeConstraint,
typeParameter */
