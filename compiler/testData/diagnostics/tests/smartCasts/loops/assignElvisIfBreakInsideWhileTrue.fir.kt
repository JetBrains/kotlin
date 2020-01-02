public fun foo(x: String?, y: String?): Int {
    while (true) {
        val z = x ?: if (y == null) break else y
        // z is not null in both branches
        z.length
        // y is nullable if x != null
        y.<!INAPPLICABLE_CANDIDATE!>length<!>
    }
    // y is null because of the break
    return y.<!INAPPLICABLE_CANDIDATE!>length<!>
}