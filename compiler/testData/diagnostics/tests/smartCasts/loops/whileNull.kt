fun foo(s: String?): Int {
    while (s==null) {
    }
    return <!DEBUG_INFO_SMARTCAST!>s<!>.length
}