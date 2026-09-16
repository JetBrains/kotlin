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
fun ignorableOp(s: String): String = s

fun nonIgnorableOp(s: String): String = s

fun testAnonymousFunction(s: String) {
    s.myLet(fun(it: String): String {
        return ignorableOp(it)
    })

    s.<!RETURN_VALUE_NOT_USED!>myLet<!>(fun(it: String): String {
        return nonIgnorableOp(it)
    })
}

/* GENERATED_FIR_TAGS: anonymousFunction, contractCallsEffect, contracts, funWithExtensionReceiver, functionDeclaration,
functionalType, inline, lambdaLiteral, nullableType, thisExpression, typeParameter */
