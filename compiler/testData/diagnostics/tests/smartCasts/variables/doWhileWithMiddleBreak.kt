fun x(): Boolean { return true }

public fun foo(pp: Any): Int {
    var p = pp
    do {
        (p as String).length
        if (p == "abc") break
        p = 42
    } while (!x())
    // Smart cast is NOT possible here
    return p.<!UNRESOLVED_REFERENCE!>length<!>()
}