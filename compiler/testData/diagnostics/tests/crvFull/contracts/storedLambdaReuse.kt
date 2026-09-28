// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
// OPT_IN: kotlin.contracts.ExperimentalContracts
// KT-85408 returnsResultOf loses ignorable-result analysis for function reference stored in local variable

import kotlin.contracts.*

inline fun <T, R> T.myLet(block: (T) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        returnsResultOf(block)
    }
    return block(this)
}

inline fun <T, R> T.myRun(block: T.() -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        returnsResultOf(block)
    }
    return block()
}

@IgnorableReturnValue
fun ignorableOp(s: String): String = s

fun nonIgnorableOp(s: String): String = s + "!"

fun testStoredLambdaReuse(s: String) {
    val ignBlock: (String) -> String = { ignorableOp(it) }
    val nonIgnBlock: (String) -> String = { nonIgnorableOp(it) }

    s.<!RETURN_VALUE_NOT_USED!>myLet<!>(ignBlock)
    s.<!RETURN_VALUE_NOT_USED!>myLet<!>(nonIgnBlock)

    s.<!RETURN_VALUE_NOT_USED!>myRun<!> { ignBlock(this) }
    s.<!RETURN_VALUE_NOT_USED!>myRun<!> { nonIgnBlock(this) }
}

/* GENERATED_FIR_TAGS: additiveExpression, contractCallsEffect, contracts, funWithExtensionReceiver, functionDeclaration,
functionalType, inline, lambdaLiteral, localProperty, nullableType, propertyDeclaration, stringLiteral, thisExpression,
typeParameter, typeWithExtension */
