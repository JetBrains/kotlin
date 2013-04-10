fun ifThen(x: Int?) {
    if (x!! == 0) {
        x : Int
    }
    x : Int
}

fun ifElse(x: Int?) {
    if (x!! == 0) else {
        x : Int
    }
    x : Int
}

fun ifThenElse(x: Int?) {
    if (x!! == 0) {
        x : Int
    } else {
        x : Int
    }
    x : Int
}

fun ifIs(x: Int?, cond: Boolean) {
    if (x is Int == cond) {
        <!TYPE_MISMATCH!>x<!> : Int
    }
    <!TYPE_MISMATCH!>x<!> : Int
}
