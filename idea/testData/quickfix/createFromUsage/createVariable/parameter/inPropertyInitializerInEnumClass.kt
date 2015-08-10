// "Create parameter 'foo'" "true"
// ERROR: No value passed for parameter foo
// ERROR: No value passed for parameter foo
// ERROR: No value passed for parameter foo

enum class E {
    A,
    B,
    C;

    val t: Int = <caret>foo
}