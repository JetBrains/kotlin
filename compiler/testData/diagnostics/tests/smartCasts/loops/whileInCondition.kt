fun foo(s: String?): Int {
    while (s!!.length > 0) {
        <!DEBUG_INFO_SMARTCAST!>s<!>.length
    }
    return <!DEBUG_INFO_SMARTCAST!>s<!>.length
}