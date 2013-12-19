fun foo(x: Int?): Int = x!!

fun elvis(x: Number?): Int {
    val result = (x as Int?) ?: foo(<!DEBUG_INFO_AUTOCAST!>x<!>)
    <!DEBUG_INFO_AUTOCAST!>x<!> : Int?
    return result
}


fun elvisWithRHSTypeInfo(x: Number?): Any? {
    val result = x ?: x!!
    <!TYPE_MISMATCH!>x<!> : Int?
    return result
}
