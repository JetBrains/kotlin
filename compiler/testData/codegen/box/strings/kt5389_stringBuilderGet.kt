// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
fun box(): String {
    val sb = StringBuilder("OK")
    return "${sb.get(0)}${sb[1]}"
}
