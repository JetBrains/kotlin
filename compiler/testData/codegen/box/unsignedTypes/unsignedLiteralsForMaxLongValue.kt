// WITH_UNSIGNED
// IGNORE_BACKEND: JVM_IR, JS_IR

fun box(): String {
    val maxULong = 0xFFFF_FFFF_FFFF_FFFFuL
    val zero = 0uL
    if (zero >= maxULong) return "Fail"

    return "OK"
}