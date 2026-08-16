// RUN_PIPELINE_TILL: BACKEND
@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class ExprAnn

@Target(AnnotationTarget.FUNCTION)
annotation class FunAnn

fun foo(): Int {
    val x = @ExprAnn fun() = 1
    val y = <!RUNTIME_ANNOTATION_ON_LAMBDA_IS_NOT_RETAINED!>@FunAnn<!> fun() = 2
    return x() + y()    
}

/* GENERATED_FIR_TAGS: additiveExpression, annotationDeclaration, anonymousFunction, functionDeclaration, integerLiteral,
localProperty, propertyDeclaration */
