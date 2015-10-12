public fun foo(x: String?): Int {
    loop@ while (true) {
        when (x) {
            null -> return -1
            "abc" -> return 0
            "xyz" -> return 1
            else -> break@loop
        }         
    }
    // x is not null because of the break
    // but we are not able to detect it
    return x<!UNSAFE_CALL!>.<!>length
}
