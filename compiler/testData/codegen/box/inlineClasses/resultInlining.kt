// WITH_RUNTIME
// IGNORE_BACKEND: JVM_IR

fun box(): String {
    val ok = Result.success("OK")
    return ok.getOrNull()!!
}
