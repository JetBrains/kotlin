// IGNORE_BACKEND: JS_IR
fun Int.foo(a: Int = 1): Int {
    return a
}

fun box(): String  {
    if (1.foo() != 1) return "fail"
    if (1.foo(2) != 2) return "fail"
    return "OK"
}