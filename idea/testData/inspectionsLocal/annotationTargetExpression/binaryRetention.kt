// FIX: Change @Retention to SOURCE

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.EXPRESSION<caret>)
annotation class Ann