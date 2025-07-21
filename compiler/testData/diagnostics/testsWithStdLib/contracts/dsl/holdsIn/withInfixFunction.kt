// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +HoldsInContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts
import kotlin.contracts.contract

inline infix fun Boolean.trueIn(block:()-> Unit) {
    contract { this@trueIn holdsIn block }
}

fun test(x: String?) {
    (x is String) trueIn { x.length }
}

/* GENERATED_FIR_TAGS: contractHoldsInEffect, contracts, funWithExtensionReceiver, functionDeclaration, functionalType,
infix, inline, isExpression, lambdaLiteral, nullableType, smartcast, thisExpression */
