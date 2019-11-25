// IGNORE_BACKEND_FIR: JVM_IR
fun zap(s: String) = s

inline fun tryZap1(string: String, fn: (String) -> String) =
        fn(
                try { zap(string) } finally {
                    try { zap(string) } finally { }
                }
        )

inline fun tryZap2(string: String, fn: (String) -> String) =
        fn(
                try { zap(string) } finally {
                    try { zap(string) } catch (e: Exception) { }
                }
        )

fun box(): String = tryZap1("O") { it } + tryZap2("K") { it }