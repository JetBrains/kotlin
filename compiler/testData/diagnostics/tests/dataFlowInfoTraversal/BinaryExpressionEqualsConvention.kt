trait A
trait B : A

fun foo1(a: A, b: B): Boolean {
    val result = (a as B) == b
    <!DEBUG_INFO_SMARTCAST!>a<!> : B
    return result
}

fun foo2(a: A, b: B): Boolean {
    val result = b == (a as B)
    <!DEBUG_INFO_SMARTCAST!>a<!> : B
    return result
}
