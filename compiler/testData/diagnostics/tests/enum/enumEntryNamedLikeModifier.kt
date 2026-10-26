// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-90707

enum class E1 {
    error,
    value,
    inline,
    ;
}

enum class E2(val code: String) {
    correct("correct"),
    error("error"),
    value("value"),
    inline("inline"),
    ;
}

enum class E3 {
    error {},
    value {},
    inline {},
    ;
}

enum class E4(val code: String) {
    error("error") {},
    value("value") {},
    inline("inline") {},
    ;
}

fun test() {
    E1.error
    E2.error.code
    E3.inline
    E4.value.code
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, functionDeclaration, primaryConstructor, propertyDeclaration */
