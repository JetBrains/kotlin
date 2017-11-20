// !LANGUAGE: +ReturnsEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.internal.contracts.*

fun test(x: Any?) {
    if (isString(x)) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
}

fun isString(x: Any?): Boolean {
    contract {
        returns(true) implies (x is String)
    }
    return x is String
}


