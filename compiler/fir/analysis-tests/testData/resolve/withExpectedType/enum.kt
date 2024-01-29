// LANGUAGE: +ExpectedTypeGuidedResolution

enum class Problem {
    CONNECTION, AUTHENTICATION, DATABASE
}

// 'when' conditions
fun message(problem: Problem): String = when (problem) {
    CONNECTION -> "connection"
    AUTHENTICATION -> "authentication"
    DATABASE -> "database"
}

// value initialization
data class Bee(val p: Problem = CONNECTION)

// return value
fun problematic(x: String): Problem = when (x) {
    "connection" -> CONNECTION
    "authentication" -> AUTHENTICATION
    else -> DATABASE
}