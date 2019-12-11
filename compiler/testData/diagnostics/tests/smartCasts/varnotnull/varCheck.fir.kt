fun get(): String? {
    return ""
}

fun foo(): Int {
    var c: String? = get()
    c!!.length
    return c.length // Previous line should make !! unnecessary here.
}