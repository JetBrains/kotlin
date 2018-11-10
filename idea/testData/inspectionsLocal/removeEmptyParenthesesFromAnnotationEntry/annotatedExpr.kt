@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class MyAnnotation

fun test() {
    val x = @MyAnnotation(<caret>) 5
}