// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
enum class Some {
    FIRST,
    SECOND;
}

fun foo(s: Some) = when (s) {
    FIRST -> SECOND
    SECOND -> FIRST
}
