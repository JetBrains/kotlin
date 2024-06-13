// LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// OPT_IN: kotlin.contracts.ExperimentalContracts
// DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

fun Any?.foo(): Boolean {
    contract {
        <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(true) implies (<!SENSELESS_COMPARISON!>this != null<!>)<!>
    }
    return this != null
}
