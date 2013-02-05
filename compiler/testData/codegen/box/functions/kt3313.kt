fun foo<T>(t: T) {
}

fun box(): String {
    foo(null)
    return "OK"
}
