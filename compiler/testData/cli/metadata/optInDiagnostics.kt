interface NotAnAnnotation

@Deprecated("Warning", level = DeprecationLevel.WARNING)
@RequiresOptIn
@Retention(AnnotationRetention.BINARY)
annotation class Warning

@Deprecated("Error", level = DeprecationLevel.ERROR)
@RequiresOptIn
annotation class Error