public fun foo(x: String?, y: String?): Int {
    do {
        // After the check, smart cast should work
        if (x != null) {
            if (x == "abc") break
            y!!.length
        } else {
            y!!.length
        }
        // y!! in both branches
        <!DEBUG_INFO_SMARTCAST!>y<!>.length
    } while (true)
    // break is possible before so !! is necessary
    return y!!.length
}