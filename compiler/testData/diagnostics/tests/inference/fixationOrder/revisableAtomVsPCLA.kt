// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-86043

fun <M, E> buildSomething(m: M, b: MutableCollection<E>.() -> M) {}

fun foo() {}
fun foo(x: String) {}

fun test(t: Function1<String, Unit>) {
    buildSomething(
        ::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>
    ) {
        add(1)

        t
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral, nullableType, typeParameter,
typeWithExtension */
