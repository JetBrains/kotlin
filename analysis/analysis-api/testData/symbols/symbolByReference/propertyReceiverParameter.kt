annotation class ReceiverAnnotation
@Target(AnnotationTarget.TYPE)
annotation class ReceiverTypeAnnotation

val @receiver:ReceiverAnnotation @ReceiverTypeAnnotation Long.prop: Boolean get() = { t<caret>his == 1 }
