// LANGUAGE: +ExpectedTypeGuidedResolution

enum class Some {
    FIRST,
    SECOND;
}

typealias Other = Some

fun foo(o: Other) = when (o) {
    _.FIRST -> 1
    _.SECOND -> 2
}
