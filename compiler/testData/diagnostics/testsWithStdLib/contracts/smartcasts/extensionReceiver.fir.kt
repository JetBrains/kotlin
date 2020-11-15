// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect -ContractsOnCallsWithImplicitReceiver
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER
//
// ISSUE: KT-28672

import kotlin.contracts.*

fun CharSequence?.isNullOrEmpty(): Boolean {
    contract {
        returns(false) implies (this@isNullOrEmpty != null)
    }

    return this == null || this.length == 0
}

fun smartcastOnReceiver(s: String?) {
    with(s) {
        if (isNullOrEmpty()) {
            <!INAPPLICABLE_CANDIDATE!>length<!>
        }
        else {
            length
        }
    }
}

fun mixedReceiver(s: String?) {
    if (!s.isNullOrEmpty()) {
        with(s) {
            length
        }
    } else {
        with(s) {
            <!INAPPLICABLE_CANDIDATE!>length<!>
        }
    }
}
