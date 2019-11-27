// !LANGUAGE: +ProperIeee754Comparisons
// IGNORE_BACKEND_FIR: JVM_IR

val minus: Any = -0.0

fun box(): String {
    if (minus is Comparable<*> && minus is Double) {
        if (minus < 0.0) return "fail 0"
        if ((minus) != 0.0) return "fail 1"
        if (minus != 0.0) return "fail 2"
        if (minus != 0.0F) return "fail 3"
    }
    return "OK"
}