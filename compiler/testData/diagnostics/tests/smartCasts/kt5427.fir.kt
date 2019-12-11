fun foo(p: String?): Int {
    // We should get smart cast here
    val x = if (p != null) { p } else "a"
    return x.length
}
