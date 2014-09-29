fun foo1(x: Number, cond: Boolean): Boolean {
    val result = cond && ((x as Int) == 42)
    <!TYPE_MISMATCH!>x<!> : Int
    return result
}

fun foo2(x: Number, cond: Boolean): Boolean {
    val result = ((x as Int) == 42) && cond
    <!DEBUG_INFO_SMARTCAST!>x<!> : Int
    return result
}

fun foo3(x: Number, cond: Boolean): Boolean {
    val result = cond || ((x as Int) == 42)
    <!TYPE_MISMATCH!>x<!> : Int
    return result
}

fun foo4(x: Number, cond: Boolean): Boolean {
    val result = ((x as Int) == 42) || cond
    <!DEBUG_INFO_SMARTCAST!>x<!> : Int
    return result
}
