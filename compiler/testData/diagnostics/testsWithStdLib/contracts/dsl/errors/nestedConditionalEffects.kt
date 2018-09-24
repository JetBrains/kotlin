// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

fun foo(boolean: Boolean) {
    <!ERROR_IN_CONTRACT_DESCRIPTION(Error in contract description)!>contract<!> {
        (returns() implies (boolean)) <!UNRESOLVED_REFERENCE!>implies<!> (!boolean)
    }
}