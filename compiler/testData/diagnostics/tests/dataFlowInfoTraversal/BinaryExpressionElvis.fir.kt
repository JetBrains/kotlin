// !CHECK_TYPE

fun foo(x: Int?): Int = x!!

fun elvis(x: Number?): Int {
    val result = (x as Int?) ?: foo(x)
    checkSubtype<Int?>(x)
    return result
}


fun elvisWithRHSTypeInfo(x: Number?): Any? {
    val result = x ?: x!!
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Int?>(x)
    return result
}
