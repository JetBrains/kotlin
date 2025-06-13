// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class foo

fun f(s : String?) : Boolean {
    return (@foo s?.equals("a"))!!
}

/* GENERATED_FIR_TAGS: annotationDeclaration, checkNotNullCall, functionDeclaration, nullableType, safeCall,
stringLiteral */
