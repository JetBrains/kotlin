fun x(): Boolean { return true }

public fun foo(p: String?, r: String?): Int {
    do {
        do {
            p!!.length
        } while (r == null)  
    } while (!x())
    // Auto cast possible
    r.<!INAPPLICABLE_CANDIDATE!>length<!>
    // Auto cast possible
    return p.length
}