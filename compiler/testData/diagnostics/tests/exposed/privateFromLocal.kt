// RUN_PIPELINE_TILL: BACKEND
interface Your

class My {
    // private from local: ???
    private val x = object : Your {}

    // private from local: ???
    private fun foo() = {
        class Local
        Local()
    }()
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, functionDeclaration, interfaceDeclaration,
lambdaLiteral, localClass, propertyDeclaration */
