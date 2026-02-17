// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

fun returnsLambdaWithCallsInPlace(block: () -> Unit): () -> Unit {
    contract {
        callsInPlace(block, InvocationKind.UNKNOWN)
    }
    return block
}

fun returnsLambdaWithCallsInPlaceAtMostOnce(block: () -> Unit): () -> Unit {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return block
}

fun callsAndReturnsLambda(block: () -> Unit): (() -> Unit)? {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
    return block
}

fun callsLambdaWithoutReturn(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
}

fun conditionalReturnWithCallsInPlace(block: () -> Unit, condition: Boolean, block3 : () -> Unit): () -> Unit {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return if (condition) block else block3
}

fun multipleParametersOneReturned(block1: () -> Unit, block2: () -> Unit): () -> Unit {
    contract {
        callsInPlace(block1, InvocationKind.UNKNOWN)
        callsInPlace(block2, InvocationKind.EXACTLY_ONCE)
    }
    block2()
    return block1
}

/* GENERATED_FIR_TAGS: contractCallsEffect, contracts, functionDeclaration, functionalType, ifExpression, lambdaLiteral,
nullableType */
