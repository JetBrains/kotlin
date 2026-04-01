// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis
// ISSUE: KT-85142

@file:OptIn(ExperimentalContracts::class)

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@JvmName("fooString")
fun foo(block: () -> String) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    block()
}

fun foo(block: () -> Int) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    block()
}

fun main() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!> { "" }
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classReference, contractCallsEffect, contracts, functionDeclaration,
functionalType, lambdaLiteral, stringLiteral */
