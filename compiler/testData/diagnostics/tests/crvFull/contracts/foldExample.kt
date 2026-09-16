// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

class MyResult<T>(private val value: T?, private val error: Throwable?) {
    fun exceptionOrNull(): Throwable? = error
    fun getOrNull(): T? = value
}

inline fun <R, T> MyResult<T>.myFold(
    onSuccess: (value: T) -> R,
    onFailure: (exception: Throwable) -> R
): R {
    contract {
        callsInPlace(onSuccess, InvocationKind.AT_MOST_ONCE)
        returnsResultOf(onSuccess)
        callsInPlace(onFailure, InvocationKind.AT_MOST_ONCE)
        returnsResultOf(onFailure)
    }
    return when (val e = exceptionOrNull()) {
        null -> onSuccess(getOrNull() <!UNCHECKED_CAST!>as T<!>)
        else -> onFailure(e)
    }
}

fun fallback(): String = "fallback"

fun testFoldAllIgnorable(ok: MyResult<String>, fail: MyResult<String>, sb: StringBuilder) {
    ok.myFold(
        { sb.append(it) },
        { return }
    )

    fail.myFold(
        { sb.append(it) },
        { return }
    )
}

fun testFoldMixedBranches(ok: MyResult<String>, fail: MyResult<String>, sb: StringBuilder) {
    ok.<!RETURN_VALUE_NOT_USED!>myFold<!>(
    { sb.append(it) },
    { fallback() }
    )

    fail.<!RETURN_VALUE_NOT_USED!>myFold<!>(
    { sb.append(it) },
    { fallback() }
    )
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, contractCallsEffect, contracts, equalityExpression, flexibleType,
funWithExtensionReceiver, functionDeclaration, functionalType, inline, javaFunction, lambdaLiteral, localProperty,
nullableType, primaryConstructor, propertyDeclaration, smartcast, stringLiteral, typeParameter, whenExpression,
whenWithSubject */
