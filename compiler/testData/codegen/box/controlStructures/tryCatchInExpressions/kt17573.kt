// IGNORE_BACKEND_FIR: JVM_IR
fun zap(s: String) = s

inline fun tryZap(string: String, fn: (String) -> String) =
        fn(try { zap(string) } finally {})

fun box(): String = tryZap("OK") { it }