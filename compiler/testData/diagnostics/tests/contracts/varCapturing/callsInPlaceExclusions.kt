// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

inline fun barInline(f: () -> Unit) {
    f()
}

@OptIn(ExperimentalContracts::class)
fun barWithContract(f: () -> Unit) {
    contract {
        callsInPlace(f, InvocationKind.EXACTLY_ONCE)
    }
    f()
}

fun baz(s: String?) {}

fun testInlineExclusion() {
    var x : String? = "bla"

    barInline {
        baz(x)
    }

    x = null
}

fun testContractExclusion() {
    var x : String? = "bla"

    barWithContract {
        baz(x)
    }

    x = null
}

fun testStandartInlineFunctionExclusion() {
    var sum = 0
    val numbers = listOf(1, 2, 3, 4)

    numbers.forEach {
        sum += it
    }

    sum += 100
    println(sum)
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, classReference, contractCallsEffect, contracts,
functionDeclaration, functionalType, inline, integerLiteral, lambdaLiteral, localProperty, multiplicativeExpression,
nullableType, propertyDeclaration, stringLiteral */
