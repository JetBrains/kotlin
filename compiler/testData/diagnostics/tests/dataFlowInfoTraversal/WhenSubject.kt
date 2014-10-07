fun foo(x: Number) {
    when (x as Int) {
        else -> <!DEBUG_INFO_SMARTCAST!>x<!> : Int
    }
    <!DEBUG_INFO_SMARTCAST!>x<!> : Int
}
