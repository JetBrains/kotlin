// IGNORE_BACKEND_FIR: JVM_IR
fun zap(s: String) = s

inline fun tryZap(s1: String, s2: String, fn: (String, String) -> String) =
        fn(
                try { zap(s1) } catch (e: Exception) { "" },
                try { zap(s2) } catch (e: Exception) { "" }
        )

fun box(): String = tryZap("O", "K") { a, b -> a + b }