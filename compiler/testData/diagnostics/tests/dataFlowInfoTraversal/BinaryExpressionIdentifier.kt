// !CHECK_TYPE

fun foo(a: Number): Int {
    val result = (a as Int) compareTo <!DEBUG_INFO_SMARTCAST!>a<!>
    checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>a<!>)
    return result
}

fun bar(a: Number): Int {
    val result = 42 compareTo (a as Int)
    checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>a<!>)
    return result
}
