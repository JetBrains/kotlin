// DO_NOT_CHECK_SYMBOL_RESTORE_K2

annotation class ReceiverAnnotation
@Target(AnnotationTarget.TYPE)
annotation class ReceiverTypeAnnotation

fun @receiver:ReceiverAnnotation @ReceiverTypeAnnotation Int.foo() {
    thi<caret>s
}
