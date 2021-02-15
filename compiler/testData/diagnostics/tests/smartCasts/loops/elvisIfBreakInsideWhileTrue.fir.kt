public fun foo(x: String?, y: String?): Int {
    while (true) {
        x ?: <!INVALID_IF_AS_EXPRESSION!>if<!> (y == null) break
        // y is nullable if x != null
        y<!UNSAFE_CALL!>.<!>length
    }
    // y is null because of the break
    return y<!UNSAFE_CALL!>.<!>length
}
