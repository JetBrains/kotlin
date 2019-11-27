// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

fun box(): String {
    val ok = Result.success("OK")
    return ok.getOrNull()!!
}
