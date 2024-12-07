// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
fun foo() = @ann 1

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class ann