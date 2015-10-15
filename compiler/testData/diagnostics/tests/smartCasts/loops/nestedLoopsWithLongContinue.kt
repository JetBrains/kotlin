fun x(): Boolean { return true }

public fun foo(p: String?, r: String?, q: String?): Int {
    outer@ while(true) {
        q!!.length
        do {
            if (x()) continue@outer
            do {
                p!!.length
            } while (!x())
        } while (r == null)
        if (!x()) break
    }
    // Smart cast is possible only for q
    <!DEBUG_INFO_SMARTCAST!>q<!>.length
    // But not possible for the others
    r<!UNSAFE_CALL!>.<!>length
    return p<!UNSAFE_CALL!>.<!>length
}