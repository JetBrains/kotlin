// ISSUE: KT-75316
// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

enum class Problem {
    CONNECTION, AUTHENTICATION, DATABASE, UNKNOWN
}

fun message(problem: Problem): String = when (problem) {
    CONNECTION -> "connection"
    AUTHENTICATION -> "authentication"
    DATABASE -> "database"
    UNKNOWN -> "unknown"
}

fun problematic(x: String): Problem = when (x) {
    "connection" -> CONNECTION
    "authentication" -> AUTHENTICATION
    "database" -> DATABASE
    else -> UNKNOWN
}

fun box(): String {
    if (problematic(message(AUTHENTICATION)) != AUTHENTICATION) return "fail 1"
    if (problematic(message(UNKNOWN)) != UNKNOWN) return "fail 2"
    return "OK"
}
