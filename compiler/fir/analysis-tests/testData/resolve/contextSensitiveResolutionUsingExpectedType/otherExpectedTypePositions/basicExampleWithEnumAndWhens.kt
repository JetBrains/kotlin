// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-75316
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

enum class Problem {
    CONNECTION, AUTHENTICATION, DATABASE, UNKNOWN
}

fun message(problem: Problem): String = <!WHEN_ON_SEALED_GEEN_ELSE!>when (problem) {
    CONNECTION -> "connection"
    AUTHENTICATION -> "authentication"
    DATABASE -> "database"
    UNKNOWN -> "unknown"
}<!>

fun problematic(x: String): Problem = when (x) {
    "connection" -> CONNECTION
    "authentication" -> AUTHENTICATION
    "database" -> DATABASE
    else -> UNKNOWN
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, equalityExpression, functionDeclaration, smartcast, stringLiteral,
whenExpression, whenWithSubject */
