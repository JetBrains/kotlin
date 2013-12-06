fun ifThen(x: Int?) {
    if (x!! == 0) {
        <!DEBUG_INFO_AUTOCAST!>x<!> : Int
    }
    <!DEBUG_INFO_AUTOCAST!>x<!> : Int
}

fun ifElse(x: Int?) {
    if (x!! == 0) else {
        <!DEBUG_INFO_AUTOCAST!>x<!> : Int
    }
    <!DEBUG_INFO_AUTOCAST!>x<!> : Int
}

fun ifThenElse(x: Int?) {
    if (x!! == 0) {
        <!DEBUG_INFO_AUTOCAST!>x<!> : Int
    } else {
        <!DEBUG_INFO_AUTOCAST!>x<!> : Int
    }
    <!DEBUG_INFO_AUTOCAST!>x<!> : Int
}

fun ifIs(x: Int?, cond: Boolean) {
    if ((x is Int) == cond) {
        <!TYPE_MISMATCH!>x<!> : Int
    }
    <!TYPE_MISMATCH!>x<!> : Int
}
