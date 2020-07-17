// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect -AllowContractsForNonOverridableMembers
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

class Foo(val x: Int?) {
    fun isXNull(): Boolean {
        contract {
            returns(false) implies (<!UNRESOLVED_REFERENCE!>x<!> != null)
        }
        return x != null
    }
}