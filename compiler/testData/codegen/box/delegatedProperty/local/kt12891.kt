// IGNORE_BACKEND_FIR: JVM_IR
//WITH_RUNTIME
fun box(): String {
    val x by lazy { "OK" }
    return x
}