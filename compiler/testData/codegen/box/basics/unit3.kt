fun box(): String {
    val actual = foo(Unit)
    if (actual != "kotlin.Unit") return "FAIL: $actual"
    return "OK"
}

fun foo(x: Any): String {
    return x.toString()
}
