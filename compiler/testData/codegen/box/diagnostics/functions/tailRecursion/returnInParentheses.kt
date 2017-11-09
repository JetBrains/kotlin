// IGNORE_BACKEND_WITHOUT_CHECK: JS

tailrec fun foo(x: Int) {
    if (x == 0) return
    (return foo(x - 1))
}

fun box(): String {
    foo(1000000)
    return "OK"
}