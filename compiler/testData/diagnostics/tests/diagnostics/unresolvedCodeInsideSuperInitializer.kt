// RUN_PIPELINE_TILL: FRONTEND

class A()

abstract class StringSpecMy(body: () -> Unit) {
    init {
        body()
    }
}

class ProblemSpec : StringSpecMy(
    {
        val params = object {}
        val a = <!UNRESOLVED_REFERENCE!>NoSuchClass<!>()
        val b = A().<!UNRESOLVED_REFERENCE!>noSuchMethod<!>()
    }
)

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, functionalType, init, lambdaLiteral, localProperty,
primaryConstructor, propertyDeclaration */
