// ISSUE: KT-75316
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
