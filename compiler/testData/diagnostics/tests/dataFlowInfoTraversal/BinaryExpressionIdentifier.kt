// !CHECK_TYPE

infix fun Int.<!EXTENSION_SHADOWED_BY_MEMBER!>compareTo<!>(<!UNUSED_PARAMETER!>o<!>: Int) = 0

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