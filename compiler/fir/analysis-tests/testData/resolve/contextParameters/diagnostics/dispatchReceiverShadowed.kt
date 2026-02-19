// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
// ISSUE: KT-79770
class Foo {
    fun foo() {}

    fun usage() {
        context(Foo()) {
            <!RECEIVER_SHADOWED_BY_CONTEXT_PARAMETER!>foo<!>()
        }
    }

    context(_: Foo)
    fun usage2() {
        <!RECEIVER_SHADOWED_BY_CONTEXT_PARAMETER!>foo<!>()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext, lambdaLiteral */
