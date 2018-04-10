// FIX: Change @Retention to SOURCE

@Retention(AnnotationRetention.RUNTIME)
@Target(<caret>AnnotationTarget.EXPRESSION)
annotation class Ann