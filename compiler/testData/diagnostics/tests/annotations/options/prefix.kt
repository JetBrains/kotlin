// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
@Target(AnnotationTarget.FUNCTION)
annotation class FunAnn

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class ExprAnn

fun foo(): Int {
    var x = 5
    <!WRONG_ANNOTATION_TARGET!>@FunAnn<!> ++x
    @ExprAnn ++x
    return x
}

/* GENERATED_FIR_TAGS: annotationDeclaration, assignment, functionDeclaration, incrementDecrementExpression,
integerLiteral, localProperty, propertyDeclaration */
