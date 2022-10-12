annotation class ReceiverAnnotation
@Target(AnnotationTarget.TYPE)
annotation class ReceiverTypeAnnotation

val @receiver:ReceiverAnnotation @ReceiverTypeAnnotation Long.prop: Boolean get() = { t<caret>his == 1 }

// DO_NOT_CHECK_SYMBOL_RESTORE_K2