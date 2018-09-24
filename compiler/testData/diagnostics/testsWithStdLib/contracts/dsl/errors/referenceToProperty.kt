// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

class Foo(val x: Int?) {
    fun isXNull(): Boolean {
        <!CONTRACT_NOT_ALLOWED!>contract<!> {
            returns(false) implies (x != null)
        }
        return x != null
    }
}