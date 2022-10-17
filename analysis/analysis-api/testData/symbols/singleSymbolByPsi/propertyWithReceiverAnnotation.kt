annotation class ReceiverAnnotation
@Target(AnnotationTarget.TYPE)
annotation class ReceiverTypeAnnotation

val @receiver:ReceiverAnnotation @ReceiverTypeAnnotation Long.pr<caret>op: Boolean get() = true
