fun bar(): Boolean { return true }

public fun foo(x: String?): Int {
    var y: Any
    do {
        // This and hashCode() below are needed just to prevent 
        // UNINITIALIZED_VARIABLE, UNUSED_VALUE, ...
        y = "" 
        y = if (x == null) break else <!DEBUG_INFO_SMARTCAST!>x<!>
    } while (bar())
    y.hashCode()
    // x is null because of the break
    return x<!UNSAFE_CALL!>.<!>length
}
