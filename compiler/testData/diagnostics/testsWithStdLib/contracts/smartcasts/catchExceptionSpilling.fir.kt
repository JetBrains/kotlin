// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

fun myAssert(condition: Boolean) {
    contract {
        returns() implies (condition)
    }
    if (!condition) throw kotlin.IllegalArgumentException("Assertion failed")
}

fun testWithCatch(x: Any?) {
    try {
        myAssert(x is String)
        x.<!UNRESOLVED_REFERENCE!>length<!>
    } catch (e: java.lang.IllegalArgumentException) { }

    x.<!UNRESOLVED_REFERENCE!>length<!>
}