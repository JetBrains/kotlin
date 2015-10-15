fun x(): Boolean { return true }

public fun foo(pp: String?, rr: String?): Int {
    var p = pp
    var r = rr
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