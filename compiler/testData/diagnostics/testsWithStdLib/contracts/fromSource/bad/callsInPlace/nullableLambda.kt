// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// ISSUE: KT-82880

import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun runExactlyOnce(block: (() -> Unit)?) {
    <!ERROR_IN_CONTRACT_DESCRIPTION!>contract {
        <!WRONG_INVOCATION_KIND!>callsInPlace(block, InvocationKind.EXACTLY_ONCE)<!>
    }<!>
    block?.invoke()
}

@OptIn(ExperimentalContracts::class)
fun runAtLeastOnce(block: (() -> Unit)?) {
    <!ERROR_IN_CONTRACT_DESCRIPTION!>contract {
        <!WRONG_INVOCATION_KIND!>callsInPlace(block, InvocationKind.AT_LEAST_ONCE)<!>
    }<!>
    block?.invoke()
}

/* GENERATED_FIR_TAGS: classReference, contractCallsEffect, contracts, functionDeclaration, functionalType,
lambdaLiteral, nullableType, safeCall */
