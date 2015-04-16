public fun foo(x: String?, y: String?): Int {
    while (true) {
        (if (x != null) break else y) ?: y!!
        // x is not null in both branches
        <!DEBUG_INFO_SMARTCAST!>y<!>.length()
    }
    // y can be null because of the break
    return y<!UNSAFE_CALL!>.<!>length()
}