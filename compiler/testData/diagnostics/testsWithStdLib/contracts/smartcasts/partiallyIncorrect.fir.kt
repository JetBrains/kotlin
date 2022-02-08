// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !OPT_IN: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

fun isString(x: Any?): Boolean {
    contract {
        returns(true) implies (x is String)
    }
    return x is String
}

fun incorrectPartDoesntMatter(x: Any?) {
    if (isString(x) && <!CONDITION_TYPE_MISMATCH!>1<!>) {
        x.length
    }
    else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}