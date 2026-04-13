// RUN_PIPELINE_TILL: BACKEND
fun foo() = @ann 1

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class ann

/* GENERATED_FIR_TAGS: annotationDeclaration, functionDeclaration, integerLiteral */
