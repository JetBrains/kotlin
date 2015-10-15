fun x(): Boolean { return true }

public fun foo(p: String?): Int {
    // KT-6284
    while(true) {
        p!!.length
        if (x()) break
    }
    // while (true) loop body is executed at least once
    // so p is not null here
    return <!DEBUG_INFO_SMARTCAST!>p<!>.length
}