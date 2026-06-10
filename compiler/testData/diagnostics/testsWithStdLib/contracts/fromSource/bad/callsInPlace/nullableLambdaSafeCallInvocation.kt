// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// ISSUE: KT-78919

import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun callTwiceAtMostOnce(block: (() -> Unit)?) {
    contract {
        <!WRONG_INVOCATION_KIND!>callsInPlace(block, InvocationKind.AT_MOST_ONCE)<!>
    }
    block?.invoke()
    block?.invoke()
}

/* GENERATED_FIR_TAGS: classReference, contractCallsEffect, contracts, functionDeclaration, functionalType,
lambdaLiteral, nullableType, safeCall */
