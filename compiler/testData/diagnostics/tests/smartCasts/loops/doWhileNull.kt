fun foo(s: String?): Int {
    do {
    } while (s==null)
    return <!DEBUG_INFO_SMARTCAST!>s<!>.length
}