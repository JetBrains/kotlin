// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +AllowCheckForErasedTypesInContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

inline fun <reified T> foo(v: Any?, action: (T) -> Unit) {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
        returns() implies (v is T)
    }
    require(v is T)
    action(v)
}

fun usageFoo(x: Any?) {
    var captured: String? = null
    foo<String>(x) { captured = it }
    captured?.length
}

@Suppress("UNCHECKED_CAST")
fun <T> bar(v: Any?, action: (T) -> Unit) {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    if (v is <!CANNOT_CHECK_FOR_ERASED!>T<!>)
    (action as (Any?) -> Unit)(v)
}

/* GENERATED_FIR_TAGS: asExpression, assignment, contractCallsEffect, contractConditionalEffect, contracts,
functionDeclaration, functionalType, ifExpression, inline, isExpression, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, reified, safeCall, smartcast, stringLiteral, typeParameter */
