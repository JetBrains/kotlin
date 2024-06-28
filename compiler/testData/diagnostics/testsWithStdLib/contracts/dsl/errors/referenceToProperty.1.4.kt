// LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect +AllowContractsForNonOverridableMembers
// OPT_IN: kotlin.contracts.ExperimentalContracts
// DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

class Foo(val x: Int?) {
    fun isXNull(): Boolean {
        contract {
            returns(false) implies (<!ERROR_IN_CONTRACT_DESCRIPTION!>x<!> != null)
        }
        return x != null
    }
}