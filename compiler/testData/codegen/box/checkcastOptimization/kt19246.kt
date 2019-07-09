// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

enum class ResultType constructor(val reason: String) {
    SOMETHING("123"),
    OK("OK"),
    UNKNOWN("FAIL");

    companion object {
        fun getByVal(reason: String): ResultType {
            return ResultType.values().firstOrDefault({ it.reason == reason }, UNKNOWN)
        }
    }
}

inline fun <T> Array<out T>.firstOrDefault(predicate: (T) -> Boolean, default: T): T {
    return firstOrNull(predicate) ?: default
}

fun box(): String = ResultType.getByVal("OK").reason
