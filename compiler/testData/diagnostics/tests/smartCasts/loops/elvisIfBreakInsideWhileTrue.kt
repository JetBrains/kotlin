public fun foo(x: String?, y: String?): Int {
    while (true) {
        x ?: if (y == null) break
        // y is not null in both branches
        <!DEBUG_INFO_SMARTCAST!>y<!>.length()
    }
    // y is null because of the break
    return y<!UNSAFE_CALL!>.<!>length()
}