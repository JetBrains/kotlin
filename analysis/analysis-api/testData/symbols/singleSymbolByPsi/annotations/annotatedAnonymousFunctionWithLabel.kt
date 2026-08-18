@Target(AnnotationTarget.FUNCTION)
annotation class MyAnnotation

fun usage() {
    val x = @MyAnnotation label@ fu<caret>n() {

    }
}
