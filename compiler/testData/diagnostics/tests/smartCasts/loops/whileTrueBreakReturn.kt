fun x(): Boolean { return true }

public fun foo(p: String?): Int {
    while(true) {
        if (x()) break
        if (p==null) return -1
        // p is not null
        <!DEBUG_INFO_SMARTCAST!>p<!>.length
    }
    // p can be null because break is earlier than return
    return p<!UNSAFE_CALL!>.<!>length
}