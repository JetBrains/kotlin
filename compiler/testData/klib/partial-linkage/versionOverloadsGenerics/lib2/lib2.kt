fun computeFoo(): String {
    val c = C()
    val s = c.foo<String>(10)
    return if (s == "10/foo/false") "OK" else "FAIL"
}

fun computeBar(): String {
    val c = C()
    val s = c.bar("x", 7)
    return if (s == "x/7/x/7/x:7/true") "OK" else "FAIL"
}