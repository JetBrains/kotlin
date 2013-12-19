trait A

trait B : A
fun B.compareTo(b: B) = if (this == b) 0 else 1

fun foo(a: A): Boolean {
    val result = (a as B) < <!DEBUG_INFO_AUTOCAST!>a<!>
    <!DEBUG_INFO_AUTOCAST!>a<!> : B
    return result
}

fun bar(a: A, b: B): Boolean {
    val result = b < (a as B)
    <!DEBUG_INFO_AUTOCAST!>a<!> : B
    return result
}
