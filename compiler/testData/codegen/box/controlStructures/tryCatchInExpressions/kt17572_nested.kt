// IGNORE_BACKEND_FIR: JVM_IR
fun zap(s: String) = s

inline fun tryZap(string: String, fn: (String) -> String) =
        fn(
                try {
                    try {
                        zap(string)
                    }
                    catch (e: Exception) { "" }
                } catch (e: Exception) { "" }
        )

fun box(): String = tryZap("OK") { it }