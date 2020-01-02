fun bar(): Boolean { return true }

fun gav(arg: Any, z: String): String { return if (arg is String) arg else z }

public fun foo(x: String?, z: String?): Int {
    do {
        gav(if (x == null) break else x, z!!)
    } while (bar())
    // z is nullable despite of z!!
    z.<!INAPPLICABLE_CANDIDATE!>length<!>
    // x is null because of the break
    return x.<!INAPPLICABLE_CANDIDATE!>length<!>
}
