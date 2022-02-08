@Target(AnnotationTarget.TYPE)
annotation class TypeAnnotation(val value: Int)

fun x(): List<@TypeAnnotation(1) I<caret>nt> {
    TODO()
}