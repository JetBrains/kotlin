// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*


fun nullWhenString(x: Any?): Int? {
    contract {
        returns(null) implies (x is String)
    }
    return if (x is String) null else 42
}

fun nullWhenNotString(x: Any?): Int? {
    contract {
        returns(null) implies (x !is String)
    }
    return if (x !is String) null else 42
}





// ==== Actual tests =====


fun test1(x: Any?) {
    // condition == true <=> function returned null <=> 'x' is String
    if (nullWhenString(x) == null) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
    else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun test2(x: Any?) {
    // Observe that condition == false <=>* function returned null <=> 'x' is String
    // *correct only for at most binary types, which is exactly the case for nullability comparisons
    if (nullWhenString(x) != null) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
    else {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
}


fun test3(x: Any?) {
    // condition == false <=> function returned not-null, but we don't know anything about when function returns not-null
    if (nullWhenNotString(x) == null) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
    else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}