fun x(): Boolean { return true }

public fun foo(p: String?, r: String?, q: String?): Int {
    while(true) {
        q!!.length
        do {
            p!!.length
        } while (r == null)
        if (!x()) break
    }
    // Smart cast is possible everywhere
    <!DEBUG_INFO_SMARTCAST!>r<!>.length
    <!DEBUG_INFO_SMARTCAST!>q<!>.length
    return <!DEBUG_INFO_SMARTCAST!>p<!>.length
}