public fun foo(p: String?, y: String?): Int {
    do {
        // After the check, smart cast should work
        if (y == null) break
        <!DEBUG_INFO_SMARTCAST!>y<!>.length
        p!!.length
    } while (true)
    return y?.length ?: -1
}