// IDE_MODE
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
// WITH_STDLIB

// FILE: main.kt
package main

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun check(block1: () -> Unit, block2: () -> Unit) {
    contract {
        callsInPlace(block1, InvocationKind.EXACTLY_ONCE)
        callsInPlace(block2, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }

    block1()
    block2()
}
