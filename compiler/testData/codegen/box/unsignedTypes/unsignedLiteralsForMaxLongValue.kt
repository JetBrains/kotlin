// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
// IGNORE_BACKEND: JVM_IR

fun box(): String {
    val maxULong = 0xFFFF_FFFF_FFFF_FFFFuL
    val zero = 0uL
    if (zero >= maxULong) return "Fail"

    return "OK"
}