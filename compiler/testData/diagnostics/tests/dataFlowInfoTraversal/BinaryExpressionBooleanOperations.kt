// !CHECK_TYPE

fun foo1(x: Number, cond: Boolean): Boolean {
    val result = cond && ((x as Int) == 42)
    checkSubtype<Int>(<!TYPE_MISMATCH!>x<!>)
    return result
}

fun foo2(x: Number, cond: Boolean): Boolean {
    val result = ((x as Int) == 42) && cond
    checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>x<!>)
    return result
}

fun foo3(x: Number, cond: Boolean): Boolean {
    val result = cond || ((x as Int) == 42)
    checkSubtype<Int>(<!TYPE_MISMATCH!>x<!>)
    return result
}

fun foo4(x: Number, cond: Boolean): Boolean {
    val result = ((x as Int) == 42) || cond
    checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>x<!>)
    return result
}
