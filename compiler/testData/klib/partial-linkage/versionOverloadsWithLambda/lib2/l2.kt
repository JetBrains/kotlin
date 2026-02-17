fun computeFoo(): String {
    val c = C()
    val l: (String) -> String = String::uppercase
    val s = c.foo(10, l)
    return if (s == "10/B/false".uppercase()) "OK" else "FAIL1"
}

fun computeBar(): String {
    val c = C()
    val l: (String) -> String = String::uppercase
    val s = c.bar(10, l)
    return if (s == "10/A1/false".uppercase()) "OK" else "FAIL2"
}