// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
package foo

fun dispatch(request: Request) {
    val url = request.getRequestURI() as String

    if (request.getMethod()?.length != 0) {
    }
}

interface Request {
    fun getRequestURI(): String?
    fun getMethod(): String?
}

/* GENERATED_FIR_TAGS: asExpression, equalityExpression, functionDeclaration, ifExpression, integerLiteral,
interfaceDeclaration, localProperty, nullableType, propertyDeclaration, safeCall */
