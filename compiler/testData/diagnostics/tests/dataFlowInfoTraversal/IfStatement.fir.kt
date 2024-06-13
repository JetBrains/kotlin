// CHECK_TYPE

fun ifThen(x: Int?) {
    if (x!! == 0) {
        checkSubtype<Int>(x)
    }
    checkSubtype<Int>(x)
}

fun ifElse(x: Int?) {
    if (x!! == 0) else {
        checkSubtype<Int>(x)
    }
    checkSubtype<Int>(x)
}

fun ifThenElse(x: Int?) {
    if (x!! == 0) {
        checkSubtype<Int>(x)
    } else {
        checkSubtype<Int>(x)
    }
    checkSubtype<Int>(x)
}

fun ifIs(x: Int?, cond: Boolean) {
    if ((x is Int) == cond) {
        checkSubtype<Int>(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
    }
    checkSubtype<Int>(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
}
