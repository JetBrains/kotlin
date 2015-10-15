fun x(p: String): Boolean { return p == "abc" }

public fun foo(p: String?, r: String?, q: String?): Int {
    while(true) {
        q!!.length
        loop@ do {
            while(true) {
                p!!.length
                if (x(<!DEBUG_INFO_SMARTCAST!>p<!>)) break@loop
                if (x(<!DEBUG_INFO_SMARTCAST!>q<!>)) break
            }
        } while (r == null)
        if (!x(<!DEBUG_INFO_SMARTCAST!>p<!>)) break
    }
    // Long break allows r == null
    r<!UNSAFE_CALL!>.<!>length
    // Smart cast is possible
    <!DEBUG_INFO_SMARTCAST!>q<!>.length
    return <!DEBUG_INFO_SMARTCAST!>p<!>.length
}