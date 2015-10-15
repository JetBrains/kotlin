fun x(): Boolean { return true }

public fun foo(p: String?, r: String?): Int {
    outer@ do {
        do {
            p!!.length
            if (!x()) continue@outer
        } while (r == null)  
    } while (!x())
    // Auto cast NOT possible due to long continue
    r<!UNSAFE_CALL!>.<!>length
    // Auto cast possible
    return <!DEBUG_INFO_SMARTCAST!>p<!>.length
}