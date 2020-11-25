// JVM_TARGET: 1.8
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME
// IGNORE_BACKEND_FIR: JVM_IR

fun box(): String {
    val x = 3UL % 2U
    return if (x == 1UL) "OK" else "Fail: $x"
}
