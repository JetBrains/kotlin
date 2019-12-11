fun bar(): Boolean { return true }

fun gav(z: String, w: String, arg: Any): String 
{ return if (arg is String) arg else if (z != "") z else w }

public fun foo(x: String?, z: String?, w: String?): Int {
    do {
        gav(z!!, w!!, if (x == null) break else x)
    } while (bar())
    // w is not null because of w!!
    w.length
    // z is not null because of z!!
    z.length
    // x is null because of the break
    return x.<!INAPPLICABLE_CANDIDATE!>length<!>
}
