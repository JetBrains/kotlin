// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !OPT_IN: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

fun safeIsString(x: Any?): Boolean? {
    contract {
        returns(true) implies (x is String)
    }
    return x?.let { it is String }
}




fun equalsTrue(x: Any?) {
    if (safeIsString(x) == true) {
        x.length
    }
    else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun equalsFalse(x: Any?) {
    if (safeIsString(x) == false) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
    else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun equalsNull(x: Any?) {
    if (safeIsString(x) == null) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
    else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun notEqualsTrue(x: Any?) {
    if (safeIsString(x) != true) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
    else {
        x.length
    }
}

fun notEqualsFalse(x: Any?) {
    if (safeIsString(x) != false) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
    else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun notEqualsNull(x: Any?) {
    if (safeIsString(x) != null) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
    else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}
