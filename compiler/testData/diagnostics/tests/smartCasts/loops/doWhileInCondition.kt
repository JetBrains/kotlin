fun foo(s: String?): Int {
    do {
    } while (s!!.length > 0)
    return <!DEBUG_INFO_SMARTCAST!>s<!>.length
}