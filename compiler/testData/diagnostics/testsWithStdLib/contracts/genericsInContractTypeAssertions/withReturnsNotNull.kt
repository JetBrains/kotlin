// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// LANGUAGE: +AllowCheckForErasedTypesInContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts

import kotlin.contracts.*
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
fun <T : Any> ensure(k: KClass<T>, v: Any?): T? {
    contract { returnsNotNull() implies (v is T) }
    return if (k.isInstance(v)) v as T else null
}

fun test(x: Any?) {
    val s = ensure(String::class, x)
    s?.length
}

/* GENERATED_FIR_TAGS: asExpression, classReference, contractConditionalEffect, contracts, functionDeclaration,
ifExpression, isExpression, lambdaLiteral, localProperty, nullableType, propertyDeclaration, safeCall, typeConstraint,
typeParameter */
