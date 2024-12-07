// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// OPT_IN: kotlin.contracts.ExperimentalContracts
// DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER
// FIR_DUMP

import kotlin.contracts.InvocationKind

inline fun foo(block: () -> Unit) {
    kotlin.contracts.contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
}

fun main() {
    val x: Int
    foo {
        x = 42
    }
    println(x)
}