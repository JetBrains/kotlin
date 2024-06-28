// LANGUAGE: +ContextSensitiveEnumResolutionInWhen
enum class Some {
    FIRST,
    SECOND;
}

typealias Other = Some

fun foo(o: Other) = when (o) {
    FIRST -> 1
    SECOND -> 2
}
