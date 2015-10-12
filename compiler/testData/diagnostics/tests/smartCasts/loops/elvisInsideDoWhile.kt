public fun foo(x: String?): Int {
    do {
        // After the check, smart cast should work
        x ?: x!!.length
        // x is not null in both branches
        if (<!DEBUG_INFO_SMARTCAST!>x<!>.length == 0) break
    } while (true)
    return <!DEBUG_INFO_SMARTCAST!>x<!>.length
}