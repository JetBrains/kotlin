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

class O {
    val o = "O"
}
class K {
    val k = "K"
}

context(o: O)
fun <T> K.f(g: context(O) K.(Foo) -> T) = g(o, this, Foo())

fun test1() = with(O()) {
    K().f { <!RECEIVER_SHADOWED_BY_CONTEXT_PARAMETER!>o<!> + k }
}

fun test2() = with(O()) {
    K().f { this@with.o + k }
}

fun test3() = with(O()) {
    K().f { contextOf<O>().o + k }
}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionDeclarationWithContext, functionalType, lambdaLiteral, nullableType, propertyDeclaration, stringLiteral,
thisExpression, typeParameter, typeWithContext, typeWithExtension */
