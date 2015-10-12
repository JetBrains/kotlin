fun bar(): Boolean { return true }

fun gav(z: String, w: String, arg: Any): String 
{ return if (arg is String) <!DEBUG_INFO_SMARTCAST!>arg<!> else if (z != "") z else w }

public fun foo(x: String?, z: String?, w: String?): Int {
    do {
        gav(z!!, w!!, if (x == null) break else <!DEBUG_INFO_SMARTCAST!>x<!>)
    } while (bar())
    // w is not null because of w!!
    <!DEBUG_INFO_SMARTCAST!>w<!>.length
    // z is not null because of z!!
    <!DEBUG_INFO_SMARTCAST!>z<!>.length
    // x is null because of the break
    return x<!UNSAFE_CALL!>.<!>length
}
