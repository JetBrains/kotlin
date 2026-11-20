// TARGET_BACKEND: WASM
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

// WASM_CHECK_INSTRUCTION_NOT_IN_FUNCTION: instruction=if inFunction=message
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=br_table inFunction=message
// WASM_COUNT_INSTRUCTION_IN_FUNCTION: instruction=if inFunction=problematic count=8

// WASM_CHECK_CONTAINS_NO_CALLS: inFunction=empty
// WASM_CHECK_CALLED_IN_FUNCTION: shouldBeCalled=problematic inFunction=box
// WASM_CHECK_CALLED_IN_FUNCTION: shouldBeCalled=message inFunction=box
// WASM_CHECK_NOT_CALLED_IN_FUNCTION: shouldNotBeCalled=box inFunction=message

fun empty (){}

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
