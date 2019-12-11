public fun foo(x: String?): Int {
    do {
        // After the check, smart cast should work
        x ?: x!!.length
        // x is not null in both branches
        if (x.length == 0) break
    } while (true)
    return x.length
}