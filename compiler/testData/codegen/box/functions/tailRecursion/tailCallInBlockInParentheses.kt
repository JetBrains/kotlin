tailRecursive fun foo(x: Int) {
    return if (x > 0) {
        (foo(x - 1))
    }
    else Unit
}

fun box(): String {
    foo(1000000)
    return "OK"
}
