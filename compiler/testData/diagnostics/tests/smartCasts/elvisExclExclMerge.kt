fun test(x: Any?): Any {
    val z = x ?: x!!
    // x is not null in both branches
    <!DEBUG_INFO_SMARTCAST!>x<!>.hashCode()
    return z
}
