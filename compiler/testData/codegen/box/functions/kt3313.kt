// IGNORE_BACKEND_FIR: JVM_IR
fun <T> foo(t: T) {
}

fun box(): String {
    foo(null)
    return "OK"
}
