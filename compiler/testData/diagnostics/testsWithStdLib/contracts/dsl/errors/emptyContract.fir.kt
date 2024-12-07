// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// OPT_IN: kotlin.contracts.ExperimentalContracts
// DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER
// FIR_DUMP

import kotlin.contracts.*

fun emptyContract() {
    <!ERROR_IN_CONTRACT_DESCRIPTION!>contract { }<!>
}
