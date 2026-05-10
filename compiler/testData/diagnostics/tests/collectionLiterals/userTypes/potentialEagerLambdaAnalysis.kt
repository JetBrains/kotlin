// LANGUAGE: +CollectionLiterals +EagerLambdaAnalysis
// RUN_PIPELINE_TILL: FRONTEND

class A {
    companion object {
        operator fun of(lam: () -> A): A = A()
        operator fun of(vararg lams: () -> A): A = A()
    }
}

class B {
    companion object {
        operator fun of(lam: () -> B): B = B()
        operator fun of(vararg lams: () -> B): B = B()
    }
}

fun take(a: A) { }
fun take(b: B) { }

fun test() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>take<!>(<!UNRESOLVED_REFERENCE!>[{ A() }]<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, functionalType, lambdaLiteral,
objectDeclaration, operator, vararg */
