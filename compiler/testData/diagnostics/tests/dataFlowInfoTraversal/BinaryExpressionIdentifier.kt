// !CHECK_TYPE

infix fun Int.equals(<!UNUSED_PARAMETER!>o<!>: Int) = false

fun foo(a: Number): Boolean {
    val result = (a as Int) equals <!DEBUG_INFO_SMARTCAST!>a<!>
    checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>a<!>)
    return result
}

fun bar(a: Number): Boolean {
    val result = 42 equals (a as Int)
    checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>a<!>)
    return result
}