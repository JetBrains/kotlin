// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

inline fun <T, R> T.myLet(block: (T) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        returnsResultOf(block)
    }
    return block(this)
}

@IgnorableReturnValue
fun ignorableOp(s: String): String = s

fun nonIgnorableOp(s: String): String = s

fun testIgnorableReferences(s: String) {
    val sb = StringBuilder()
    val list = mutableListOf<String>()
    s.myLet(sb::append)
    s.myLet(list::add)
}

fun testNonIgnorableReference(s: String) {
    s.<!RETURN_VALUE_NOT_USED!>myLet<!>(String::length)
}

fun testTopLevelFunctionReferences(s: String) {
    s.<!RETURN_VALUE_NOT_USED!>myLet<!>(::nonIgnorableOp)
    s.myLet(::ignorableOp)
}

/* GENERATED_FIR_TAGS: contractCallsEffect, contracts, flexibleType, funWithExtensionReceiver, functionDeclaration,
functionalType, inline, javaCallableReference, javaFunction, lambdaLiteral, nullableType, stringLiteral, thisExpression,
typeParameter */
