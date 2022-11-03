// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// FIR_IDENTICAL
// !LANGUAGE: +RestrictRetentionForExpressionAnnotations

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class TestRetentionSource

@Target(AnnotationTarget.EXPRESSION)
<!RESTRICTED_RETENTION_FOR_EXPRESSION_ANNOTATION_ERROR!>@Retention(AnnotationRetention.BINARY)<!>
annotation class TestRetentionBinary

@Target(AnnotationTarget.EXPRESSION)
<!RESTRICTED_RETENTION_FOR_EXPRESSION_ANNOTATION_ERROR!>@Retention(AnnotationRetention.RUNTIME)<!>
annotation class TestRetentionRuntime
