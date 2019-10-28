// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

val arr = intArrayOf()

fun box(): String {
    val s = StringBuilder()
    for ((index, x) in arr.withIndex()) {
        return "Loop over empty array should not be executed"
    }
    return "OK"
}