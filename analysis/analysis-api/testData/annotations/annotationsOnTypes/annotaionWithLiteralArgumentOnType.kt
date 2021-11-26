@Target(AnnotationTarget.TYPE)
annotation class TypeAnnotation(val value: Int)

fun x(): @TypeAnnotation(1) Li<caret>st<Int> {
    TODO()
}