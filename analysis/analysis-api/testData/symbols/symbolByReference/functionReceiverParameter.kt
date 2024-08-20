// DO_NOT_CHECK_SYMBOL_RESTORE_K1
annotation class ReceiverAnnotation
@Target(AnnotationTarget.TYPE)
annotation class ReceiverTypeAnnotation

fun @receiver:ReceiverAnnotation @ReceiverTypeAnnotation Int.foo() {
    thi<caret>s
}
