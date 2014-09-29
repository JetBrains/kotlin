trait A

trait B : A
fun B.plus(b: B) = if (this == b) b else this

fun foo(a: A): B {
    val result = (a as B) + <!DEBUG_INFO_SMARTCAST!>a<!>
    <!DEBUG_INFO_SMARTCAST!>a<!> : B
    return result
}

fun bar(a: A, b: B): B {
    val result = b + (a as B)
    <!DEBUG_INFO_SMARTCAST!>a<!> : B
    return result
}
