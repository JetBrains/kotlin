// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// OPT_IN: kotlin.contracts.ExperimentalContracts
// DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

inline fun <T> myRun(block: () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}

fun functionWithExpressionBody(x: Int): Boolean = myRun {
    if (x == 0) return true
    if (x == 1) return false
    return functionWithExpressionBody(x - 2)
}

/* GENERATED_FIR_TAGS: additiveExpression, contractCallsEffect, contracts, equalityExpression, functionDeclaration,
functionalType, ifExpression, inline, integerLiteral, lambdaLiteral, nullableType, typeParameter */
