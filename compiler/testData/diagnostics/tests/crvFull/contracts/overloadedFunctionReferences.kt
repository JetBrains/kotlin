// RUN_PIPELINE_TILL: CODEGEN
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
fun overloaded(s: String): String = s

fun overloaded(i: Int): Int = i + 1

fun testOverloadedStringReference(s: String) {
    s.myLet(::overloaded)
}

fun testOverloadedIntReference(i: Int) {
    i.<!RETURN_VALUE_NOT_USED!>myLet<!>(::overloaded)
}

/* GENERATED_FIR_TAGS: additiveExpression, callableReference, contractCallsEffect, contracts, funWithExtensionReceiver,
functionDeclaration, functionalType, inline, integerLiteral, lambdaLiteral, nullableType, thisExpression, typeParameter */
