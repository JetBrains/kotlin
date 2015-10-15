fun foo(x: String): String? = x

fun calc(x: String?): Int {
    // Smart cast because of x!! in receiver
    foo(x!!)?.subSequence(0, <!DEBUG_INFO_SMARTCAST!>x<!>.length)
    // Smart cast because of x!! in receiver
    return <!DEBUG_INFO_SMARTCAST!>x<!>.length
}
