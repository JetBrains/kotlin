// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// OPT_IN: kotlin.contracts.ExperimentalContracts
// DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

fun foo(boolean: Boolean) {
    contract {
        <!ERROR_IN_CONTRACT_DESCRIPTION!>(returns() implies (boolean)) <!OPT_IN_USAGE_ERROR, UNRESOLVED_REFERENCE_WRONG_RECEIVER!>implies<!> (!boolean)<!>
    }
}