annotation class ReceiverAnnotation
@Target(AnnotationTarget.TYPE)
annotation class ReceiverTypeAnnotation

fun @receiver:ReceiverAnnotation @ReceiverTypeAnnotation Int.foo() {}

val @receiver:ReceiverAnnotation @ReceiverTypeAnnotation Long.prop: Boolean get() = true
