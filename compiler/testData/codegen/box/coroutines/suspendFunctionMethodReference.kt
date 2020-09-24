// IGNORE_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR

fun f(a: suspend () -> Unit): String {
    val f = a::invoke
    return "OK"
}

fun box(): String {
    return f {}
}
