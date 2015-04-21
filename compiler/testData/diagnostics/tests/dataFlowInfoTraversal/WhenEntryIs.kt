// !CHECK_TYPE

fun foo(x: Number, y: Int) {
    when (x) {
        is Int -> checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>x<!>)
        y -> {}
        else -> {}
    }
    checkSubtype<Int>(<!TYPE_MISMATCH!>x<!>)
}

fun bar(x: Number) {
    when (x) {
        is Int -> checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>x<!>)
        else -> {}
    }
    checkSubtype<Int>(<!TYPE_MISMATCH!>x<!>)
}

fun whenWithoutSubject(x: Number) {
    when {
        (x is Int) -> checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>x<!>)
        else -> {}
    }
    checkSubtype<Int>(<!TYPE_MISMATCH!>x<!>)
}
