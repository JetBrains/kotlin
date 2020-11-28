// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: EXCEPTIONS_NOT_IMPLEMENTED
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