// LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// OPT_IN: kotlin.contracts.ExperimentalContracts
// DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

fun foo(boolean: Boolean) {
    contract {
        <!ERROR_IN_CONTRACT_DESCRIPTION!>(returns() implies (boolean)) <!UNRESOLVED_REFERENCE!>implies<!> (!boolean)<!>
    }
}