// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextReceivers, -ContextParameters
// ISSUE: KT-76527

fun foo(f: context(String) Int.() -> Unit) {
    f("", 1)
    with("") {
        f(1)
        1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>f<!>()
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral, stringLiteral,
typeWithContext, typeWithExtension */
