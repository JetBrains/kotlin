// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

fun box(): String = when {
    0 in intArrayOf(1, 2, 3) -> "fail 1"
    1 !in intArrayOf(1, 2, 3) -> "fail 2"
    else -> "OK"
}