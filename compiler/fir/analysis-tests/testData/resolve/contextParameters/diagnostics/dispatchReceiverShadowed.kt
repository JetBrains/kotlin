// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters
// ISSUE: KT-79770
class Foo {
    fun foo() {}

    fun usage() {
        context(Foo()) {
            foo()
        }
    }

    context(_: Foo)
    fun usage2() {
        foo()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext, lambdaLiteral */
