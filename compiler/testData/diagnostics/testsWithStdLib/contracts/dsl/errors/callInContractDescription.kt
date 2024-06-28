// LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// OPT_IN: kotlin.contracts.ExperimentalContracts
// DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

fun bar(x: Int): Boolean = x == 0

fun foo(x: Int): Boolean {
    contract {
        returns(true) implies (<!ERROR_IN_CONTRACT_DESCRIPTION("call-expressions are not supported yet")!>bar(x)<!>)
    }
    return x == 0
}