// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !OPT_IN: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

fun safeIsString(x: Any?): Boolean? {
    <!WRONG_IMPLIES_CONDITION!>contract {
        returns(true) implies (x is String)
    }<!>
    return x?.let { it is String }
}




fun equalsTrue(x: Any?) {
    if (safeIsString(x) == true) {
        x.length
    }
    else {
        <!ARGUMENT_TYPE_MISMATCH!>x.<!UNRESOLVED_REFERENCE!>length<!><!>
    }
}

fun equalsFalse(x: Any?) {
    if (safeIsString(x) == false) {
        <!ARGUMENT_TYPE_MISMATCH!>x.<!UNRESOLVED_REFERENCE!>length<!><!>
    }
    else {
        x.length
    }
}

fun equalsNull(x: Any?) {
    if (safeIsString(x) == null) {
        <!ARGUMENT_TYPE_MISMATCH!>x.<!UNRESOLVED_REFERENCE!>length<!><!>
    }
    else {
        <!ARGUMENT_TYPE_MISMATCH!>x.<!UNRESOLVED_REFERENCE!>length<!><!>
    }
}

fun notEqualsTrue(x: Any?) {
    if (safeIsString(x) != true) {
        <!ARGUMENT_TYPE_MISMATCH!>x.<!UNRESOLVED_REFERENCE!>length<!><!>
    }
    else {
        x.length
    }
}

fun notEqualsFalse(x: Any?) {
    if (safeIsString(x) != false) {
        x.length
    }
    else {
        <!ARGUMENT_TYPE_MISMATCH!>x.<!UNRESOLVED_REFERENCE!>length<!><!>
    }
}

fun notEqualsNull(x: Any?) {
    if (safeIsString(x) != null) {
        <!ARGUMENT_TYPE_MISMATCH!>x.<!UNRESOLVED_REFERENCE!>length<!><!>
    }
    else {
        <!ARGUMENT_TYPE_MISMATCH!>x.<!UNRESOLVED_REFERENCE!>length<!><!>
    }
}
