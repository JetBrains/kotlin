// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

fun foo(b: Boolean): Boolean {
    contract {
        // pointless, can be reduced to just "b"
        returns(true) implies (b == true)
    }

    return b
}

fun bar(b: Boolean?): Boolean {
    contract {
        // not pointless, but not supported yet
        returns(true) implies (b == true)
    }
    if (b == null) throw java.lang.IllegalArgumentException("")
    return b
}