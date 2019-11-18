// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

fun box(): String {
    var result = "FAIL"
    val f = { result = "OK" }
    val r = Runnable(f)
    r.run()
    return result
}
