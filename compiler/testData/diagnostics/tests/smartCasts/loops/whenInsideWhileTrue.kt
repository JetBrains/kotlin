public fun foo(x: String?): Int {
    loop@ while (true) {
        when (x) {
            null -> break@loop
            "abc" -> return 0
            "xyz" -> return 1
            else -> <!DEBUG_INFO_SMARTCAST!>x<!>.length
        }         
    }
    // x is null because of the break
    return x<!UNSAFE_CALL!>.<!>length
}
