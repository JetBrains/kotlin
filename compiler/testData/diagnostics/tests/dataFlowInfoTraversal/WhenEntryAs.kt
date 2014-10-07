fun foo(x: Number, y: Int) {
    when (x) {
        x as Int -> <!DEBUG_INFO_SMARTCAST!>x<!> : Int
        y -> {}
        else -> {}
    }
    <!DEBUG_INFO_SMARTCAST!>x<!> : Int
}

fun bar(x: Number) {
    when (x) {
        x as Int -> <!DEBUG_INFO_SMARTCAST!>x<!> : Int
        else -> {}
    }
    <!DEBUG_INFO_SMARTCAST!>x<!> : Int
}

fun whenWithoutSubject(x: Number) {
    when {
        (x as Int) == 42 -> <!DEBUG_INFO_SMARTCAST!>x<!> : Int
        else -> {}
    }
    <!DEBUG_INFO_SMARTCAST!>x<!> : Int
}
