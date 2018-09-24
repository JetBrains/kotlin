// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect +UseReturnsEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

fun callsAndInverts(b: Boolean, block: () -> Unit): Boolean {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        returns(true) implies (!b)
        returns(false) implies b
    }

    block()
    return !b
}



fun smartcastAndInitialization(x: Any?) {
    val y: Int

    if (callsAndInverts(x !is String) { y = 42 }) {
        println(y)
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
    else {
        println(y)
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
    println(y)
}

fun inPresenceOfLazy(x: Any?, unknownBoolean: Boolean) {
    val y: Int

    if (unknownBoolean && callsAndInverts(x !is String) { y = 42 }) {
        println(y)
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
    else {
        println(y)
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
    println(y)
}