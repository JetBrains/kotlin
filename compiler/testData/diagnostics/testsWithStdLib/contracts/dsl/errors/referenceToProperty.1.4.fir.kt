// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect +AllowContractsForNonOverridableMembers
// !OPT_IN: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

class Foo(val x: Int?) {
    fun isXNull(): Boolean {
        contract {
            <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(false) implies (<!UNRESOLVED_REFERENCE!>x<!> != null)<!>
        }
        return x != null
    }
}