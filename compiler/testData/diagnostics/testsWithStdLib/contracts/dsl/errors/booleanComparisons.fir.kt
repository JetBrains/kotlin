// LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// OPT_IN: kotlin.contracts.ExperimentalContracts
// DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

fun foo(b: Boolean): Boolean {
    contract {
        // pointless, can be reduced to just "b"
        <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(true) implies (b == true)<!>
    }

    return b
}

fun bar(b: Boolean?): Boolean {
    contract {
        // not pointless, but not supported yet
        <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(true) implies (b == true)<!>
    }
    if (b == null) throw java.lang.IllegalArgumentException("")
    return b
}