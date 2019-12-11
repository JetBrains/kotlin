public fun foo(x: String?): Int {
    loop@ while (true) {
        when (x) {
            null -> break@loop
            "abc" -> return 0
            "xyz" -> return 1
            else -> x.length
        }         
    }
    // x is null because of the break
    return x.<!INAPPLICABLE_CANDIDATE!>length<!>
}
