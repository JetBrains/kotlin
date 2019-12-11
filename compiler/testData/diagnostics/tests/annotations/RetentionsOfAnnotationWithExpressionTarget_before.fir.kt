// !LANGUAGE: -RestrictRetentionForExpressionAnnotations

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class TestRetentionSource

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.BINARY)
annotation class TestRetentionBinary

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.RUNTIME)
annotation class TestRetentionRuntime