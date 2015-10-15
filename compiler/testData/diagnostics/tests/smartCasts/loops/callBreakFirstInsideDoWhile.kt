fun bar(): Boolean { return true }

fun gav(arg: Any, z: String): String { return if (arg is String) <!DEBUG_INFO_SMARTCAST!>arg<!> else z }

public fun foo(x: String?, z: String?): Int {
    do {
        gav(if (x == null) break else <!DEBUG_INFO_SMARTCAST!>x<!>, z!!)
    } while (bar())
    // z is nullable despite of z!!
    z<!UNSAFE_CALL!>.<!>length
    // x is null because of the break
    return x<!UNSAFE_CALL!>.<!>length
}
