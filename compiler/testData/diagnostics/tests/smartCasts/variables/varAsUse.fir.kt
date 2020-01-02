fun get(): Any {
    return ""
}

fun foo(): Int {
    var c: Any = get()
    (c as String).length
    return c.length // Previous line should make as unnecessary here.
}