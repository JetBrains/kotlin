fun bar(): Boolean { return true }

fun gav(w: String, arg: Any, z: String): String 
{ return if (arg is String) <!DEBUG_INFO_SMARTCAST!>arg<!> else if (z != "") z else w }

public fun foo(x: String?, z: String?, w: String?): Int {
    do {
        gav(w!!, if (x == null) break else <!DEBUG_INFO_SMARTCAST!>x<!>, z!!)
    } while (bar())
    // w is not null because of w!!
    <!DEBUG_INFO_SMARTCAST!>w<!>.length
    // z is nullable despite of z!!
    z<!UNSAFE_CALL!>.<!>length
    // x is null because of the break
    return x<!UNSAFE_CALL!>.<!>length
}
