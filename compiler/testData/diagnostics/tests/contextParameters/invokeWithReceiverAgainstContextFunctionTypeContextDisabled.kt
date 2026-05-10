// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ContextParameters
// ISSUE: KT-73805

fun test(f: <!UNSUPPORTED_FEATURE!>context(String, Int)<!> Boolean.() -> Unit) {
    "".<!UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL, UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL!>f<!>(1, true)
    with("") {
        with(1) {
            <!NO_VALUE_FOR_PARAMETER, UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL, UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL!>f<!>(<!ARGUMENT_TYPE_MISMATCH!>true<!>)
        }
    }
    <!UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL!>f<!>("", 1, true)
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral, stringLiteral,
typeWithContext, typeWithExtension */
