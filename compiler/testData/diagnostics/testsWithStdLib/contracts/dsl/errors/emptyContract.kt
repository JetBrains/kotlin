// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.contracts.ExperimentalContracts
// DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER
// FIR_DUMP

import kotlin.contracts.*

fun emptyContract() {
    <!ERROR_IN_CONTRACT_DESCRIPTION!>contract { }<!>
}

/* GENERATED_FIR_TAGS: contracts, functionDeclaration, lambdaLiteral */
