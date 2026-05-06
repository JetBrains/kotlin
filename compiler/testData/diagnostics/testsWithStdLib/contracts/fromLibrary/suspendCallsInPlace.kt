// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-63416

import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
suspend fun <Result> callOnceSuspending(block: suspend () -> Result): Result {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    try {
        return block()
    } finally {
        println("some cleanup")
    }
}

/* GENERATED_FIR_TAGS: classReference, contractCallsEffect, contracts, functionDeclaration, functionalType,
lambdaLiteral, nullableType, stringLiteral, suspend, tryExpression, typeParameter */
