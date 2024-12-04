// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// Result type can be annotated
@Target(AnnotationTarget.TYPE)
annotation class My(val x: Int)

fun foo(): @My(42) Int = 24