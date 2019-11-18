// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

fun test(a: Any?) {
    a as () -> Unit
    Runnable(a).run()
}

fun box(): String {
    var result = "Fail"
    test {
        result = "OK"
    }
    return result
}
