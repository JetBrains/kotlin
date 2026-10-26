// COMPILATION_ERRORS
// ISSUE: KT-90707
enum class E1(x: String) {
    @Ann inline(""),
    public value {},
    private error("") {},
    ;
}

// `context` followed by `(` is still parsed as a context parameter list, not as an entry name
enum class E2(x: String) {
    context(""),
    ;
}
