// RUN_PIPELINE_TILL: FRONTEND

class A()

abstract class StringSpecMy(body: () -> Unit) {
    init {
        body()
    }
}

class MyProblemSpec : StringSpecMy(
    {
        val params = object {}
        {
            A().<!UNRESOLVED_REFERENCE!>noSuchMethod<!>()
        }
    }
)

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, functionalType, init, lambdaLiteral, localProperty,
primaryConstructor, propertyDeclaration */
