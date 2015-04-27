public fun foo(x: String?): Int {
    var y: Any
    loop@ while (true) {
        y = when (x) {
            null -> break@loop
            "abc" -> return 0
            "xyz" -> return 1
            else -> <!DEBUG_INFO_SMARTCAST!>x<!>.length()
        }         
        // y is always Int after when
        <!DEBUG_INFO_SMARTCAST!>y<!>: Int
    }    
    // x is null because of the break
    return x<!UNSAFE_CALL!>.<!>length()
}
