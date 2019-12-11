fun test(x: Any?): Any {
    val z = x ?: x!!
    // x is not null in both branches
    x.hashCode()
    return z
}
