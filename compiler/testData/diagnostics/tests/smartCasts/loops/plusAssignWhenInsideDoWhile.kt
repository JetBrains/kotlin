fun bar(): Boolean { return true }

public fun foo(x: String?): Int {
    var y: Int?
    y = 0
    loop@ do {
        <!DEBUG_INFO_SMARTCAST!>y<!> += when (x) {
            null -> break@loop
            "abc" -> return 0
            "xyz" -> return 1
            else -> <!DEBUG_INFO_SMARTCAST!>x<!>.length()
        }         
        // y is always Int after when
        <!DEBUG_INFO_SMARTCAST!>y<!>: Int
    } while (bar())
    // y is always Int even here
    <!DEBUG_INFO_SMARTCAST!>y<!>: Int
    // x is null because of the break
    return x<!UNSAFE_CALL!>.<!>length()
}
