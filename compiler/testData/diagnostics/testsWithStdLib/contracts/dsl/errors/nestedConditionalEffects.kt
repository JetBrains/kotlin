// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// OPT_IN: kotlin.contracts.ExperimentalContracts
// DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

fun foo(boolean: Boolean) {
    <!ERROR_IN_CONTRACT_DESCRIPTION("Error in contract description")!>contract<!> {
        (returns() implies (boolean)) <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>implies<!> (!boolean)
    }
}