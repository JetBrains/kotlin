public fun foo(x: String?, y: String?): Int {
    while (true) {
        (if (x != null) break else y) ?: y!!
        // y is not null in both branches but it's hard to determine
        y<!UNSAFE_CALL!>.<!>length
    }
    // y can be null because of the break
    return y<!UNSAFE_CALL!>.<!>length
}