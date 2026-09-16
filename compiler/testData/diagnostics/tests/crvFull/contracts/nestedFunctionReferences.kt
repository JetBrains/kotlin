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

fun nonIgnorableOp(s: String): String = s + "!"

fun testNestedReferences(a: String?, b: String?) {
    a?.myLet {
        b?.myLet(::ignorableOp)
    }

    a?.<!RETURN_VALUE_NOT_USED!>myLet<!> {
        b?.myLet(::nonIgnorableOp)
    }
}

/* GENERATED_FIR_TAGS: additiveExpression, callableReference, contractCallsEffect, contracts, funWithExtensionReceiver,
functionDeclaration, functionalType, inline, lambdaLiteral, nullableType, safeCall, stringLiteral, thisExpression,
typeParameter */
