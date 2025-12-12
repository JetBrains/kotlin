// LANGUAGE: +ContextParameters
// MODULE: original
@Target(AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)
annotation class MyAnnotation

context(x: Int)
fun foo() {}

// MODULE: copy
@Target(AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)
annotation class MyAnnotation

context(@MyAnnotation x: Int)
fun foo() {}