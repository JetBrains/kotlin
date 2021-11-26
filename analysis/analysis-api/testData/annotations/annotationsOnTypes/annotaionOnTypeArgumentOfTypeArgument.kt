@Target(AnnotationTarget.TYPE)
annotation class TypeAnnotation

fun x(): List<MutableList<@TypeAnnotation I<caret>nt>> {
    TODO()
}