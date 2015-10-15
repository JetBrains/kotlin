fun bar(): Boolean { return true }

fun foo(s: String?): Int {
    while (s!!.length > 0) {
        <!DEBUG_INFO_SMARTCAST!>s<!>.length
        if (bar()) break
    }
    return <!DEBUG_INFO_SMARTCAST!>s<!>.length
}