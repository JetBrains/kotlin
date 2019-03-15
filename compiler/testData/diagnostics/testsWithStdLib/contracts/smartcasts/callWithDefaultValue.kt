// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

fun myAssert(condition: Boolean, message: String = "") {
    contract {
        returns() implies (condition)
    }
    if (!condition) throw kotlin.IllegalArgumentException(message)
}

fun test(x: Any?) {
    myAssert(x is String)
    <!DEBUG_INFO_SMARTCAST!>x<!>.length
}