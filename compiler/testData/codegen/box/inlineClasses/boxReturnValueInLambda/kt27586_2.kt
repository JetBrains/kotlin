// WITH_RUNTIME
// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JVM_IR

fun f1() = lazy {
    runCatching {
        "OK"
    }
}

fun box(): String {
    val r = f1().value
    return r.getOrNull() ?: "fail: $r"
}