// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +AllowCheckForErasedTypesInContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*
import kotlin.reflect.KClass

fun <T : Any> isInstanceErasedCheck(value: Any?): Boolean {
    contract { returns(true) implies (value is T) }
    return value is <!CANNOT_CHECK_FOR_ERASED!>T<!>
}

fun usageErasedCheck(x: Any) {
    if (isInstanceErasedCheck<String>(x)) {
        x.length
    }
}

fun <T : Any> isInstanceContractOnly(value: Any?): Boolean {
    contract { returns(true) implies (value is T) }
    return true
}

fun usageContractOnly(x: Any) {
    if (isInstanceContractOnly<String>(x)) {
        x.length
    }
}

fun <T : Any> isInstanceByKClass(kClass: KClass<T>, value: Any?): Boolean {
    contract { returns(true) implies (value is T) }
    return kClass.isInstance(value)
}

fun usageKClass(x: Any) {
    if (isInstanceByKClass(String::class, x)) {
        x.length
    }
}

/* GENERATED_FIR_TAGS: classReference, contractConditionalEffect, contracts, functionDeclaration, ifExpression,
isExpression, lambdaLiteral, nullableType, smartcast, typeConstraint, typeParameter */
