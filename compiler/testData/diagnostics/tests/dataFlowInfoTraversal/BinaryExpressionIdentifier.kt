fun foo(a: Number): Int {
    val result = (a as Int) compareTo <!DEBUG_INFO_SMARTCAST!>a<!>
    <!DEBUG_INFO_SMARTCAST!>a<!> : Int
    return result
}

fun bar(a: Number): Int {
    val result = 42 compareTo (a as Int)
    <!DEBUG_INFO_SMARTCAST!>a<!> : Int
    return result
}
