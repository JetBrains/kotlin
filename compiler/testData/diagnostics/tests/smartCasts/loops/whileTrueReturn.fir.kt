fun x(): Boolean { return true }

public fun foo(p: String?): Int {
    while(true) {
        if (p==null) return -1
        if (x()) break
        // p is not null
        p.length
    }
    // while (true) loop body with return is executed at least once
    // so p is not null here
    return p.length
}