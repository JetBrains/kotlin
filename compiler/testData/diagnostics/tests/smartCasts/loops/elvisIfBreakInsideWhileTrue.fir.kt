public fun foo(x: String?, y: String?): Int {
    while (true) {
        x ?: if (y == null) break
        // y is nullable if x != null
        y.<!INAPPLICABLE_CANDIDATE!>length<!>
    }
    // y is null because of the break
    return y.<!INAPPLICABLE_CANDIDATE!>length<!>
}