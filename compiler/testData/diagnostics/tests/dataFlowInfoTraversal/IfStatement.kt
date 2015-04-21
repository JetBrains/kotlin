// !CHECK_TYPE

fun ifThen(x: Int?) {
    if (x!! == 0) {
        checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>x<!>)
    }
    checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>x<!>)
}

fun ifElse(x: Int?) {
    if (x!! == 0) else {
        checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>x<!>)
    }
    checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>x<!>)
}

fun ifThenElse(x: Int?) {
    if (x!! == 0) {
        checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>x<!>)
    } else {
        checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>x<!>)
    }
    checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>x<!>)
}

fun ifIs(x: Int?, cond: Boolean) {
    if ((x is Int) == cond) {
        checkSubtype<Int>(<!TYPE_MISMATCH!>x<!>)
    }
    checkSubtype<Int>(<!TYPE_MISMATCH!>x<!>)
}
