// !LANGUAGE: +NewInference

fun foo(s: String): Boolean {
    if (s != "kotlin") throw AssertionError(s)
    return true
}

fun bar(f: (String) -> Unit) {
    f("kotlin")
}

fun box(): String {
    bar(::foo)
    return "OK"
}
