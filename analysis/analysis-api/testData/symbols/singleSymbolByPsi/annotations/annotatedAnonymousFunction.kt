@Target(AnnotationTarget.FUNCTION)
annotation class MyAnnotation

fun usage() {
    val a = @MyAnnotation f<caret>un() {

    }
}
