// LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// OPT_IN: kotlin.contracts.ExperimentalContracts
// DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

fun myAssert(condition: Boolean) {
    contract {
        returns() implies (condition)
    }
    if (!condition) throw kotlin.IllegalArgumentException("Assertion failed")
}

fun testAssertSmartcast(x: Any?) {
    myAssert(x is String)
    x.length
}

fun testInvertedAssert(x: Any?) {
    myAssert(x !is String)
    x.<!UNRESOLVED_REFERENCE!>length<!>
}

fun testSpilling(x: Any?) {
    if (x != null) {
        myAssert(x is String)
        x.length
    }
    x<!UNSAFE_CALL!>.<!>length
}

fun testAssertInIf(x: Any?) {
    if (myAssert(x is String) == Unit) {
        x.length
    }
    else {
        x.length
    }
}

fun testTryCatch(x: Any?) {
    try {
        myAssert(x is String)
        x.length
    } catch (e: kotlin.IllegalArgumentException) {

    }
    x.<!UNRESOLVED_REFERENCE!>length<!>
}

fun testUncertainFlow(x: Any?) {
    repeat(x.toString().length) {
        myAssert(x is String)
        x.length
    }
    x.<!UNRESOLVED_REFERENCE!>length<!>
}

fun testAtLeastOnceFlow(x: Any?) {
    do {
        myAssert(x is String)
        x.length
    } while (<!SENSELESS_COMPARISON!>x != null<!>)

    x.length
}
