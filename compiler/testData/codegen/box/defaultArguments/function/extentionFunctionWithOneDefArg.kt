// IGNORE_BACKEND_FIR: JVM_IR
fun Int.foo(a: Int = 1, b: String): Int {
    return a
}

fun box(): String  {
    if (1.foo(b = "b") != 1) return "fail"
    if (1.foo(2, "b") != 2) return "fail"
    return "OK"
}