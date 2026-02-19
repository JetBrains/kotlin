// FILE: lib.kt
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

// FILE: main.kt
fun box(): String = tryZap("OK") { it }