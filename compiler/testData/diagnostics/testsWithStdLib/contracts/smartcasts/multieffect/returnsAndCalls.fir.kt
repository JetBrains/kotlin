// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect +UseCallsInPlaceEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

fun callsAndInverts(b: Boolean, block: () -> Unit): Boolean {
    contract {
        <!INAPPLICABLE_CANDIDATE!>callsInPlace<!>(block, InvocationKind.EXACTLY_ONCE)
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
        x.<!UNRESOLVED_REFERENCE!>length<!>
    } else {
        println(y)
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
    println(y)
}

fun inPresenceOfLazy(x: Any?, unknownBoolean: Boolean) {
    val y: Int

    if (unknownBoolean && callsAndInverts(x !is String) { y = 42 }) {
        println(y)
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
    else {
        println(y)
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
    println(y)
}

fun isPresenceOfLazy2(x: Any?, unknownBoolean: Boolean) {
    val y: Int
    if (unknownBoolean && callsAndInverts(x !is String) { y = 42 }) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
    else {
        println(y)
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
    println(y)
}

fun isPresenceOfLazy3(x: Any?, unknownBoolean: Boolean) {
    val y: Int
    if (unknownBoolean && callsAndInverts(x !is String) { y = 42 }) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
    else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
    println(y)
}