// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1
annotation class PropertyAnnotation
annotation class GetAnnotation
annotation class ExplicitGetAnnotation
annotation class ReceiverAnnotation

@Target(AnnotationTarget.TYPE)
annotation class ReceiverTypeAnnotation

@property:PropertyAnnotation
@get:GetAnnotation
val @receiver:ReceiverAnnotation @ReceiverTypeAnnotation Long.x: Int
    @ExplicitGetAnnotation ge<caret>t() = 1
