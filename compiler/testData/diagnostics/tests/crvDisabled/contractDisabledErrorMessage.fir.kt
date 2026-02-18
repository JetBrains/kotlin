// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

inline fun <T, R> T.myLet(block: (T) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        <!ERROR_IN_CONTRACT_DESCRIPTION("requires language feature 'return value checker' to be enabled")!>returnsResultOf(block)<!>
    }
    return block(this)
}

/* GENERATED_FIR_TAGS: contractCallsEffect, contracts, funWithExtensionReceiver, functionDeclaration, functionalType,
inline, lambdaLiteral, nullableType, thisExpression, typeParameter */
