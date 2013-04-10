fun foo(x: Int?): Int = x!!

fun elvis(x: Number?): Int {
    val result = x as Int? ?: foo(x)
    x : Int?
    return result
}


fun elvisWithRHSTypeInfo(x: Number?): Any? {
    val result = x ?: x!!
    <!TYPE_MISMATCH!>x<!> : Int?
    return result
}
