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
        x.length
    }
    else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun nested2(x: Any?) {
    myAssert(equalsTrue(isString(x)))
    x.length
}

fun nested3(x: Any?) {
    myAssert(equalsTrue(notEqualsNull(nullWhenNotString(x))))
    x.length
}

fun branchedAndNested(x: Any?, y: Any?) {
    myAssert(equalsTrue(notEqualsNull(nullWhenNotString(x))) && equalsTrue(isString(y)))
    x.length
    y.length
}


fun br(y: Any?) {
    if (myAssert(y is Int) == Unit && myAssert(y is String) == Unit) {
        y.length
        y.inc()
    }
}

fun branchedAndNestedWithNativeOperators(x: Any?, y: Any?) {
    myAssert(
            equalsTrue(notEqualsNull(nullWhenNotString(x)))   // x is String
            &&
            (
                    (myAssert(y is Int) == Unit && myAssert(y is String) == Unit)  // y is Int, String
                    ||
                    equalsTrue(isInt(y) && isString(y))                          // y is Int, String
            )
            &&
            (1 == 2 || <!USELESS_IS_CHECK!>y is Int<!> || isString(y))
    )
    x.length
    y.length
    y.inc()
}

