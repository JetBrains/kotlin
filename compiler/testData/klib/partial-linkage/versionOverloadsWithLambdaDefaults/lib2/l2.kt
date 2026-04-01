fun computeTrailing(): String {
    val c = C()
    val f: (String) -> String = String::uppercase
    val s = c.inTrailing("hello", f)
    return if (s == "hello/10/false/") "OK" else "FAIL"
}

fun computeArgument(): String {
    val c = C()
    val f: (String) -> String = String::uppercase
    val s = c.inArgument("hello", f)
    return if (s == "hello/5/6/") "OK" else "FAIL"
}