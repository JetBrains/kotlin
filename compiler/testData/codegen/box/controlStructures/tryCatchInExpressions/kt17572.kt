fun zap(s: String) = s

inline fun tryZap(string: String, fn: (String) -> String) =
        fn(try { zap(string) } catch (e: Exception) { "" })

fun box(): String = tryZap("OK") { it }