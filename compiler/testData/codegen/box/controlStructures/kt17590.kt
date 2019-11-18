// IGNORE_BACKEND_FIR: JVM_IR
fun zap(s: String): String? = s

inline fun tryZap(s: String, fn: (String) -> String): String {
    return fn(zap(s) ?: return "null")
}

fun box() = tryZap("OK") { it }