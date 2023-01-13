annotation class ReceiverAnnotation
@Target(AnnotationTarget.TYPE)
annotation class ReceiverTypeAnnotation

fun @receiver:ReceiverAnnotation @ReceiverTypeAnnotation Int.foo() {
    thi<caret>s
}
