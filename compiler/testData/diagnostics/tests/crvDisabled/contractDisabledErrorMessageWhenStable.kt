// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// OPT_IN: kotlin.contracts.ExperimentalContracts
// LANGUAGE: +ReturnValueCheckerIsStable

import kotlin.contracts.*

inline fun <T, R> T.myLet(block: (T) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        returnsResultOf(block)
    }
    return block(this)
}

/* GENERATED_FIR_TAGS: contractCallsEffect, contracts, funWithExtensionReceiver, functionDeclaration, functionalType,
inline, lambdaLiteral, nullableType, thisExpression, typeParameter */
