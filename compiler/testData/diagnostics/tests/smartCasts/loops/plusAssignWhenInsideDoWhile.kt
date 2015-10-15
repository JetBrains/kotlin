// !CHECK_TYPE

fun bar(): Boolean { return true }

public fun foo(x: String?): Int {
    var y: Int?
    y = 0
    loop@ do {
        <!DEBUG_INFO_SMARTCAST!>y<!> += when (x) {
            null -> break@loop
            "abc" -> return 0
            "xyz" -> return 1
            else -> <!DEBUG_INFO_SMARTCAST!>x<!>.length
        }         
        // y is always Int after when
        checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>y<!>)
    } while (bar())
    // y is always Int even here
    checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>y<!>)
    // x is null because of the break
    return x<!UNSAFE_CALL!>.<!>length
}
