// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +HoldsInContracts, +AllowCheckForErasedTypesInContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts
import kotlin.contracts.contract

inline fun <T> runIfIs(value: Any, block: () -> Unit) {
    contract {
        (value is T) holdsIn block
    }
}

fun test(x: Any) {
    runIfIs<String>(x) {
        x.length
    }
}

/* GENERATED_FIR_TAGS: contractHoldsInEffect, contracts, functionDeclaration, functionalType, inline, isExpression,
lambdaLiteral, nullableType, smartcast, typeParameter */
