// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +AllowCheckForErasedTypesInContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts

import kotlin.contracts.*
import kotlin.reflect.KClass

class C<T> {
    fun checkClassTypeParamError(value: Any?): Boolean {
        contract {
            <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(true) implies (value is T)<!>
        }
        return true
    }
}

fun usageNegative() {
    val c = C<String>()
    val any: Any? = ""
    if (c.checkClassTypeParamError(any)) {
        println(any.<!UNRESOLVED_REFERENCE!>length<!>)
    }
}

fun <U : Any> checkFunctionTypeParam(value: Any?): Boolean {
    contract { returns(true) implies (value is U) }
    return true
}

fun usagePositiveFunctionParam(x: Any) {
    if (checkFunctionTypeParam<String>(x)) {
        x.length
    }
}

fun <U : Any> checkByKClass(kClass: KClass<U>, value: Any?): Boolean {
    contract { returns(true) implies (value is U) }
    return kClass.isInstance(value)
}

fun usagePositiveKClass(list: List<Any>, v: Any?) {
    if (checkByKClass(String::class, v)) {
        val len = v.length
        list.forEach { it.toString() }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, contractConditionalEffect, contracts, functionDeclaration,
ifExpression, isExpression, lambdaLiteral, localProperty, nullableType, propertyDeclaration, smartcast, stringLiteral,
typeConstraint, typeParameter */
