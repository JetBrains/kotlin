// RUN_PIPELINE_TILL: BACKEND
class Some

fun foo(): () -> Boolean {
    val s = Some()
    if (true) {
        return { if (<!USELESS_IS_CHECK!>s is Some<!>) true else false }
    } else {
        return { true }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, ifExpression, isExpression, lambdaLiteral,
localProperty, propertyDeclaration */
