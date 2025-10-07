// TARGET_BACKEND: WASM
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
// WASM_CHECK_INSTRUCTION_NOT_IN_SCOPE: instruction=if scope_function=message
// WASM_CHECK_INSTRUCTION_IN_SCOPE: instruction=br_table scope_function=message
// WASM_COUNT_INSTRUCTION_IN_SCOPE: instruction=if scope_function=problematic count=8

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
