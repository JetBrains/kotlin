public fun foo(p: String?, y: String?): Int {
    do {
        // After this !!, y. should be smartcasted in loop as well as outside
        y!!.length
        if (p == null) break
        y.length
    } while (true)
    return y.length
}