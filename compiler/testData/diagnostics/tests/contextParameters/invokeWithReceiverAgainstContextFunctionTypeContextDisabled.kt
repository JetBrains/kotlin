// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ContextParameters
// ISSUE: KT-73805

fun test(f: <!UNSUPPORTED_FEATURE!>context(String, Int)<!> Boolean.() -> Unit) {
    "".f(1, true)
    with("") {
        with(1) {
            f(<!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!><!CONSTANT_EXPECTED_TYPE_MISMATCH!>true<!>)<!>
        }
    }
    f("", 1, true)
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral, stringLiteral,
typeWithContext, typeWithExtension */
