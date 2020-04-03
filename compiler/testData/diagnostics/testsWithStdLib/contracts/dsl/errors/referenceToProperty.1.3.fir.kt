// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect -AllowContractsForNonOverridableMembers
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

class Foo(val x: Int?) {
    fun isXNull(): Boolean {
        contract {
            returns(false) implies (x != null)
        }
        return x != null
    }
}