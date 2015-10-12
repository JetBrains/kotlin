fun foo(x: String): String? = x

fun calc(x: String?): Int {
    do {
        // Smart cast because of x!! in receiver
        foo(x!!)?.subSequence(0, <!DEBUG_INFO_SMARTCAST!>x<!>.length)
        // Smart cast because of x!! in receiver
        if (<!DEBUG_INFO_SMARTCAST!>x<!>.length == 0) break
    } while (true)
    // Here x is also not null
    return <!DEBUG_INFO_SMARTCAST!>x<!>.length
}
