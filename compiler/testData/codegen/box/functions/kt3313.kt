fun <T> foo(t: T) {
}

fun box(): String {
    foo(null)
    return "OK"
}
