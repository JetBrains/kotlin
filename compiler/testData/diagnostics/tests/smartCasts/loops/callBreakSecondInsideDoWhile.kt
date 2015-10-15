fun bar(): Boolean { return true }

fun gav(z: String, arg: Any): String { return if (arg is String) <!DEBUG_INFO_SMARTCAST!>arg<!> else z }

public fun foo(x: String?, z: String?): Int {
    do {
        gav(z!!, if (x == null) break else <!DEBUG_INFO_SMARTCAST!>x<!>)
    } while (bar())
    // z is not null because of z!!
    <!DEBUG_INFO_SMARTCAST!>z<!>.length
    // x is null because of the break
    return x<!UNSAFE_CALL!>.<!>length
}
