// ISSUE: KT-56744
// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !OPT_IN: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

fun myAssert(condition: Boolean) {
    contract {
        returns() implies (condition)
    }
    if (!condition) throw kotlin.IllegalArgumentException("Assertion failed")
}

fun isString(x: Any?): Boolean {
    contract {
        returns(true) implies (x is String)
    }
    return x is String
}

fun isInt(x: Any?): Boolean {
    contract {
        returns(true) implies (x is Int)
    }
    return x is Int
}

fun notEqualsNull(x: Any?): Boolean {
    contract {
        returns(true) implies (x != null)
    }
    return x != null
}

fun equalsTrue(x: Boolean): Boolean {
    contract {
        returns(true) implies x
    }
    return x == true
}

fun nullWhenNotString(x: Any?): String? {
    contract {
        returnsNotNull() implies (x is String)
    }
    return x as? String
}




// ========== Actual tests ============

fun nested1(x: Any?) {
    if (equalsTrue(isString(x))) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
    else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun nested2(x: Any?) {
    myAssert(equalsTrue(isString(x)))
    <!DEBUG_INFO_SMARTCAST!>x<!>.length
}

fun nested3(x: Any?) {
    myAssert(equalsTrue(notEqualsNull(nullWhenNotString(x))))
    <!DEBUG_INFO_SMARTCAST!>x<!>.length
}

fun branchedAndNested(x: Any?, y: Any?) {
    myAssert(equalsTrue(notEqualsNull(nullWhenNotString(x))) && equalsTrue(isString(y)))
    <!DEBUG_INFO_SMARTCAST!>x<!>.length
    <!DEBUG_INFO_SMARTCAST!>y<!>.length
}


fun br(y: Any?) {
    if (myAssert(y is Int) == Unit && myAssert(<!USELESS_IS_CHECK!>y is String<!>) == Unit) {
        <!DEBUG_INFO_SMARTCAST!>y<!>.length
        <!DEBUG_INFO_SMARTCAST!>y<!>.inc()
    }
}

fun branchedAndNestedWithNativeOperators(x: Any?, y: Any?) {
    myAssert(
            equalsTrue(notEqualsNull(nullWhenNotString(x)))   // x is String
            &&
            (
                    (myAssert(y is Int) == Unit && myAssert(<!USELESS_IS_CHECK!>y is String<!>) == Unit)  // y is Int, String
                    ||
                    equalsTrue(isInt(y) && isString(y))                          // y is Int, String
            )
            &&
            (1 == 2 || y is Int || isString(y))
    )
    <!DEBUG_INFO_SMARTCAST!>x<!>.length
    y.<!UNRESOLVED_REFERENCE!>length<!>
    y.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inc<!>()
}

