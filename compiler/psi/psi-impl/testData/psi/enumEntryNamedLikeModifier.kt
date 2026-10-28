// ISSUE: KT-90707
enum class E1 {
    error,
    value,
    inline,
    ;
}

enum class E2(x: String) {
    error(""),
    value(""),
    inline(""),
    ;
}

enum class E3 {
    error {},
    value {},
    inline {},
    ;
}

enum class E4(x: String) {
    error("") {},
    value("") {},
    inline("") {},
    ;
}
