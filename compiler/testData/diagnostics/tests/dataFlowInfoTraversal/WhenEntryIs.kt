fun foo(x: Number, y: Int) {
    when (x) {
        is Int -> <!DEBUG_INFO_AUTOCAST!>x<!> : Int
        y -> {}
        else -> {}
    }
    <!TYPE_MISMATCH!>x<!> : Int
}

fun bar(x: Number) {
    when (x) {
        is Int -> <!DEBUG_INFO_AUTOCAST!>x<!> : Int
        else -> {}
    }
    <!TYPE_MISMATCH!>x<!> : Int
}

fun whenWithoutSubject(x: Number) {
    when {
        (x is Int) -> <!DEBUG_INFO_AUTOCAST!>x<!> : Int
        else -> {}
    }
    <!TYPE_MISMATCH!>x<!> : Int
}
