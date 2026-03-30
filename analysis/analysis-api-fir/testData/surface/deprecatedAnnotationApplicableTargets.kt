@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class MyAnnotation

@MyAnnotation
fun test() {}