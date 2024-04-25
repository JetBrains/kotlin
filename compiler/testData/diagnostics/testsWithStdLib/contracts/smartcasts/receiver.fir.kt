// LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// OPT_IN: kotlin.contracts.ExperimentalContracts
// DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

fun Any?.isNull(): Boolean {
    contract {
        returns(false) implies (this@isNull != null)
    }
    return this == null
}

fun smartcastOnReceiver(x: Int?) {
    if (x.isNull()) {
        x<!UNSAFE_CALL!>.<!>inc()
    }
    else {
        x.dec()
    }
}

class UnstableReceiver {
    var x: Int? = 42

    fun smartcastOnUnstableReceiver() {
        if (x.isNull()) {
            x<!UNSAFE_CALL!>.<!>inc()
        }
        else {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.dec()
        }
    }
}

