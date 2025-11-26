// RUN_PIPELINE_TILL: BACKEND
interface Your

class My {
    internal val x = object : Your {}

    internal fun <!EXPOSED_FUNCTION_RETURN_TYPE!>foo<!>() = {
        class Local
        Local()
    }()
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, functionDeclaration, interfaceDeclaration,
lambdaLiteral, localClass, propertyDeclaration */
