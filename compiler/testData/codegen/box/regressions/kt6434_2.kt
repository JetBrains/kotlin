// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

fun box(): String {
    val p = 1 to 1
    val (e, num) = p
    val a = 1f

    return "OK"
}
