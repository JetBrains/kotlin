public fun foo(p: String?, y: String?): Int {
    do {
        // After the check, smart cast should work
        if (y == null) {
            "null".toString()
            break
        }
        y.length
        p!!.length
    } while (true)
    return y?.length ?: -1
}