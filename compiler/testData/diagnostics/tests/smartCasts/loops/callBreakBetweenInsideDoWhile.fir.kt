fun bar(): Boolean { return true }

fun gav(w: String, arg: Any, z: String): String 
{ return if (arg is String) arg else if (z != "") z else w }

public fun foo(x: String?, z: String?, w: String?): Int {
    do {
        gav(w!!, if (x == null) break else x, z!!)
    } while (bar())
    // w is not null because of w!!
    w.length
    // z is nullable despite of z!!
    z.<!INAPPLICABLE_CANDIDATE!>length<!>
    // x is null because of the break
    return x.<!INAPPLICABLE_CANDIDATE!>length<!>
}
