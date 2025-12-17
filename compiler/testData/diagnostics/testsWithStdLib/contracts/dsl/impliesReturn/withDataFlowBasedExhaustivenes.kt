// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ConditionImpliesReturnsContracts, +DataFlowBasedExhaustiveness
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts
// ISSUE: KT-79271
import kotlin.contracts.*

sealed interface Variants {
    data object A : Variants
    data object B : Variants
    fun foo(){}
}

fun ensureA(v: Variants): Variants? {
    contract {
        (v is Variants.A) implies (returnsNotNull())
    }
    return v as Variants.A
}

fun foo(v: Variants.A): String {
    return <!WHEN_ON_SEALED_GEEN_ELSE!>when (ensureA(v)) {
        is Variants.B -> "B"
        is Variants.A -> "A"
    }<!>
}

/* GENERATED_FIR_TAGS: asExpression, contractImpliesReturnEffect, contracts, data, functionDeclaration,
interfaceDeclaration, isExpression, lambdaLiteral, nestedClass, nullableType, objectDeclaration, sealed, smartcast,
stringLiteral, whenExpression, whenWithSubject */
