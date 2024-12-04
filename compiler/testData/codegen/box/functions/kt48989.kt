// TARGET_BACKEND: JVM_IR
fun box() = inlineFunctionWithDefaultArguments("OK")

inline fun inlineFunctionWithDefaultArguments(
    p0: String = try {
        "42"
    } finally {
    }, p1: Any = p0
) = p1