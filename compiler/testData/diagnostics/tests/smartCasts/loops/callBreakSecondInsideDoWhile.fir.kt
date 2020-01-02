fun bar(): Boolean { return true }

fun gav(z: String, arg: Any): String { return if (arg is String) arg else z }

public fun foo(x: String?, z: String?): Int {
    do {
        gav(z!!, if (x == null) break else x)
    } while (bar())
    // z is not null because of z!!
    z.length
    // x is null because of the break
    return x.<!INAPPLICABLE_CANDIDATE!>length<!>
}
