fun x(): Boolean { return true }

public fun foo(p: String?, r: String?): Int {
    do {
        do {
            p!!.length
        } while (r == null)  
    } while (!x())
    // Auto cast possible
    <!DEBUG_INFO_SMARTCAST!>r<!>.length
    // Auto cast possible
    return <!DEBUG_INFO_SMARTCAST!>p<!>.length
}