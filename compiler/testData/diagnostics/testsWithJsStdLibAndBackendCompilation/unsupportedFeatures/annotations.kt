// IGNORE_BACKEND: JS_IR
// TODO: fix in KT-61882
// !LANGUAGE: -RestrictRetentionForExpressionAnnotations
// !DIAGNOSTICS: -UNUSED_PARAMETER
// FIR_IDENTICAL

package foo

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.EXPRESSION)
annotation class AnnotationWithSourceRetention

<!RESTRICTED_RETENTION_FOR_EXPRESSION_ANNOTATION_WARNING!>@Retention(AnnotationRetention.BINARY)<!>
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.EXPRESSION)
annotation class AnnotationWithBinaryRetention

<!RESTRICTED_RETENTION_FOR_EXPRESSION_ANNOTATION_WARNING!>@Retention(AnnotationRetention.RUNTIME)<!>
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.EXPRESSION)
annotation class AnnotationWithRuntimeRetention

@AnnotationWithSourceRetention
class TestSource {
    @AnnotationWithSourceRetention
    fun baz(@AnnotationWithSourceRetention foo : Int) : Int {
        return (@AnnotationWithSourceRetention 1)
    }
}

@AnnotationWithBinaryRetention
class TestBinary {
    @AnnotationWithBinaryRetention
    fun baz(@AnnotationWithBinaryRetention foo : Int) : Int {
        return (<!NOT_SUPPORTED!>@AnnotationWithBinaryRetention 1<!>)
    }
}

<!RUNTIME_ANNOTATION_NOT_SUPPORTED!>@AnnotationWithRuntimeRetention<!>
class TestRuntime {
    <!RUNTIME_ANNOTATION_NOT_SUPPORTED!>@AnnotationWithRuntimeRetention<!>
    fun baz(@AnnotationWithRuntimeRetention foo : Int) : Int {
        return (<!NOT_SUPPORTED!>@AnnotationWithRuntimeRetention 1<!>)
    }
}

