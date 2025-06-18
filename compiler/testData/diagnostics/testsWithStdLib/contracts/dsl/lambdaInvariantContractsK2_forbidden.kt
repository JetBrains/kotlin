// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +HoldsInContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts

import kotlin.contracts.*

inline fun <R> runIfB(condition: Boolean, block: () -> R): R? {
    contract {
        <!ERROR_IN_CONTRACT_DESCRIPTION!>(condition == true) holdsIn block<!>
    }
    return if (condition) block() else null
}

/* GENERATED_FIR_TAGS: contracts, equalityExpression, functionDeclaration, functionalType, ifExpression, inline,
lambdaLiteral, nullableType, typeParameter */
