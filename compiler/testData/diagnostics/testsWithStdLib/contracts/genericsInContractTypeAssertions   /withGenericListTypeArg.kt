// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// LANGUAGE: +AllowCheckForErasedTypesInContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts

import kotlin.contracts.*
import kotlin.reflect.KClass

fun <T : Any> allIs(kClass: KClass<T>, list: List<*>): Boolean {
    contract { returns(true) implies (list is List<T>) }
    return list.all { kClass.isInstance(it) }
}

fun test(list: List<Any>) {
    if (allIs(String::class, list)) {
        val sum = list.sumOf { it.length }
        println(sum)
    }
}


/* GENERATED_FIR_TAGS: classReference, contractConditionalEffect, contracts, functionDeclaration, ifExpression,
isExpression, lambdaLiteral, localProperty, nullableType, propertyDeclaration, smartcast, starProjection, typeConstraint,
typeParameter */
