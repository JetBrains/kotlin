// WITH_RUNTIME
// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR

fun box(): String {
    val ok = Result.success("OK")
    return ok.getOrNull()!!
}
