fun x(p: String): Boolean { return p == "abc" }

public fun foo(p: String?, r: String?, q: String?): Int {
    while(true) {
        q!!.length
        loop@ do {
            while(true) {
                p!!.length
                if (x(p)) break@loop
                if (x(q)) break
            }
        } while (r == null)
        if (!x(p)) break
    }
    // Long break allows r == null
    r<!UNSAFE_CALL!>.<!>length
    // Smart cast is possible
    q.length
    return p.length
}