// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
// ISSUE: KT-78866

class Foo { fun foo() { } }
context(_: Foo) fun foo() { }

fun example1() {
    with(Foo()) {
        context(Foo()) {
            <!RECEIVER_SHADOWED_BY_CONTEXT_PARAMETER!>foo<!>()
        }
    }
}

fun example2() {
    with(Foo()) {
        context(Foo()) {
            this.foo()
        }
    }
}

fun example3() {
    with(Foo()) {
        context(Foo()) {
            contextOf<Foo>().foo()
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext, lambdaLiteral,
thisExpression */
