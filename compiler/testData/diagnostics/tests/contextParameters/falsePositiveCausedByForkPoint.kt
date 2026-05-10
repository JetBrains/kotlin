// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters
// ISSUE: KT-84155
context(f: Foo<Int>) // same result if you change it to Foo<String> or even Foo<Unit>
fun Bar<Unit>.forkPoints() {
    this <!UNCHECKED_CAST!>as Bar<Int><!>
    withFooNothing {
        context(f) { bar() } // works
        bar() // No context argument for '_: Foo<T>' found.
    }
}
context(f: Foo<Unit>)
fun Bar<Unit>.noForkPoints() {
    withFooNothing {
        context(f) { bar() } // works
        bar() // works
    }
}

class Foo<T>
class Bar<T>

context(_: Foo<T>)
fun <T> Bar<T>.bar() {}

fun withFooNothing(block: Foo<Nothing>.() -> Unit) {}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionDeclarationWithContext, functionalType, intersectionType, lambdaLiteral, nullableType, smartcast, thisExpression,
typeParameter, typeWithExtension */
