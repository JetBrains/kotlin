// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

fun Any?.isNull(): Boolean {
    contract {
        returns(false) implies (this@isNull != null)
    }
    return this == null
}

fun smartcastOnReceiver(x: Int?) {
    with(x) {
        if (isNull()) {
            <!UNSAFE_CALL!>inc<!>()
        }
        else {
            <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>dec<!>()
        }
    }
}

fun mixedReceiver(x: Int?): Int =
    if (!x.isNull()) {
        with (<!DEBUG_INFO_SMARTCAST!>x<!>) {
            inc()
        }
    } else {
        with (x) {
            <!UNSAFE_CALL!>dec<!>()
        }
    }