// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

var result = "FAIL"

fun getFun(): () -> Unit {
    return { result = "OK" }
}

fun box(): String {
    val r = Runnable(getFun())
    r.run()
    return result
}
