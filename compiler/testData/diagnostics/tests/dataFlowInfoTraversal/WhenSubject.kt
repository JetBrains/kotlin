fun foo(x: Number) {
    when (x as Int) {
        else -> <!DEBUG_INFO_AUTOCAST!>x<!> : Int
    }
    <!DEBUG_INFO_AUTOCAST!>x<!> : Int
}
