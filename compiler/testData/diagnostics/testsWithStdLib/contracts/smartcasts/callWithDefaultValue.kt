// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.internal.contracts.*

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