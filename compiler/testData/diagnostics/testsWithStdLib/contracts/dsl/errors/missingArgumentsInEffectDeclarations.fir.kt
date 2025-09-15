// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.contracts.ExperimentalContracts
// ISSUE: KT-80947

import kotlin.contracts.*

fun test(x: Any) {
    contract {
        callsInPlace()
        true.holdsIn()
        true.implies(a==)
        true.implies(is Int)
    }
}

/* GENERATED_FIR_TAGS: contractCallsEffect, contractHoldsInEffect, contractImpliesReturnEffect, contracts,
equalityExpression, functionDeclaration, lambdaLiteral */
