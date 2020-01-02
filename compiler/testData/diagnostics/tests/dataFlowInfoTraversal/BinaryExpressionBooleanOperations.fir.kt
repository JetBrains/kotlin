// !CHECK_TYPE

fun foo1(x: Number, cond: Boolean): Boolean {
    val result = cond && ((x as Int) == 42)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Int>(x)
    return result
}

fun foo2(x: Number, cond: Boolean): Boolean {
    val result = ((x as Int) == 42) && cond
    checkSubtype<Int>(x)
    return result
}

fun foo3(x: Number, cond: Boolean): Boolean {
    val result = cond || ((x as Int) == 42)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Int>(x)
    return result
}

fun foo4(x: Number, cond: Boolean): Boolean {
    val result = ((x as Int) == 42) || cond
    checkSubtype<Int>(x)
    return result
}
