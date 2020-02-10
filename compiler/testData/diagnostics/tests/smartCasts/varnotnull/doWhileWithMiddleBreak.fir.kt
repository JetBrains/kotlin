fun x(): Boolean { return true }

public fun foo(pp: String?): Int {
    var p = pp
    do {
        p!!.length
        if (p == "abc") break
        p = null
    } while (!x())
    // Smart cast is NOT possible here
    return p.length
}