// RUN_PIPELINE_TILL: CODEGEN
interface Your

class My {
    internal val x = object : Your {}

    internal fun foo() = {
        class Local
        Local()
    }()
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, functionDeclaration, interfaceDeclaration,
lambdaLiteral, localClass, propertyDeclaration */
