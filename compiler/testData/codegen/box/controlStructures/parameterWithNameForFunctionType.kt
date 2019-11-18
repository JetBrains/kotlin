// IGNORE_BACKEND_FIR: JVM_IR
fun <T> test(a: T, b: T, operation: (x: T) -> T) {
    operation(if (3 > 2) a else b)
}

fun box(): String {
    test(1, 1, { it })
    return "OK"
}