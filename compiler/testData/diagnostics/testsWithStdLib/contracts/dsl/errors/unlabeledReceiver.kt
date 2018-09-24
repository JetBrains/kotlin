// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

fun Any?.foo(): Boolean {
    contract {
        returns(true) implies (<!SENSELESS_COMPARISON!><!ERROR_IN_CONTRACT_DESCRIPTION(only references to parameters are allowed. Did you miss label on <this>?)!>this<!> != null<!>)
    }
    return this != null
}