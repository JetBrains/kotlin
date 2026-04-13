// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ContextParameters
// ISSUE: KT-73805

fun test(f: <!UNSUPPORTED_FEATURE!>context(String, Int)<!> Boolean.() -> Unit) {
    "".<!UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL, UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL!>f<!>(1, true)
    with("") {
        with(1) {
            <!UNRESOLVED_REFERENCE_WRONG_RECEIVER, UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL!>f<!>(true)
        }
    }
    <!UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL!>f<!>("", 1, true)
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral, stringLiteral,
typeWithContext, typeWithExtension */
