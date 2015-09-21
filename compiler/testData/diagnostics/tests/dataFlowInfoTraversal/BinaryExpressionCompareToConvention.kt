// !CHECK_TYPE

interface A

interface B : A
operator fun B.compareTo(b: B) = if (this == b) 0 else 1

fun foo(a: A): Boolean {
    val result = (a as B) < <!DEBUG_INFO_SMARTCAST!>a<!>
    checkSubtype<B>(<!DEBUG_INFO_SMARTCAST!>a<!>)
    return result
}

fun bar(a: A, b: B): Boolean {
    val result = b < (a as B)
    checkSubtype<B>(<!DEBUG_INFO_SMARTCAST!>a<!>)
    return result
}
