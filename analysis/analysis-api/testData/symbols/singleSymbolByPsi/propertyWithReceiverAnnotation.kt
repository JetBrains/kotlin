// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE
annotation class ReceiverAnnotation
@Target(AnnotationTarget.TYPE)
annotation class ReceiverTypeAnnotation

val @receiver:ReceiverAnnotation @ReceiverTypeAnnotation Long.pr<caret>op: Boolean get() = true
