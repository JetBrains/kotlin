fun x(): Boolean { return true }

public fun foo(qq: String?): Int {
    var q = qq
    while(true) {
        q!!.length
        var r = q
        do {
            var p = r
            do {
                // p = r, r = q and q is not null
                <!DEBUG_INFO_SMARTCAST!>p<!>.length
            } while (!x())
        } while (<!SENSELESS_COMPARISON!>r == null<!>) // r = q and q is not null
        if (!x()) break
    }
    // Smart cast is possible
    return <!DEBUG_INFO_SMARTCAST!>q<!>.length
}