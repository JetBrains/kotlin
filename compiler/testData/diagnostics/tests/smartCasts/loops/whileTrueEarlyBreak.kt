fun x(): Boolean { return true }

public fun foo(p: String?): Int {
    while(true) {
        if (x()) break
        // We do not always reach this statement
        p!!.length
    }
    // Here we have while (true) loop but p is nullable due to break before
    return p<!UNSAFE_CALL!>.<!>length
}