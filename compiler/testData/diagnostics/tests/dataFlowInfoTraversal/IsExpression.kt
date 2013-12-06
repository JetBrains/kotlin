fun foo(x: Number) {
    if ((x as Int) is Int) {
        <!DEBUG_INFO_AUTOCAST!>x<!> : Int
    }
    <!DEBUG_INFO_AUTOCAST!>x<!> : Int
}
