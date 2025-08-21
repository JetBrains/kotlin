// RUN_PIPELINE_TILL: BACKEND
// OPT_IN: kotlin.contracts.ExperimentalContracts
// LANGUAGE: +DataFlowBasedExhaustiveness

import kotlin.contracts.*

sealed interface Variants {
    data object A : Variants
    data object B : Variants
}

fun ensureNotA(v: Variants) {
    contract {
        returns() implies (v !is Variants.A)
    }
    if (v is Variants.A) throw Exception("Forbidden")
}

fun foo(v: Variants): String {
    ensureNotA(v)

    return <!NO_ELSE_IN_WHEN!>when<!> (v) {
        is Variants.B -> "B"
    }
}

/* GENERATED_FIR_TAGS: contractConditionalEffect, contracts, data, functionDeclaration, ifExpression,
interfaceDeclaration, isExpression, lambdaLiteral, nestedClass, objectDeclaration, sealed, smartcast, stringLiteral,
whenExpression, whenWithSubject */
