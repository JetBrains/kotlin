// !DIAGNOSTICS: -UNUSED_PARAMETER

package foo

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.EXPRESSION)
annotation class AnnotationWithSourceRetention

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.EXPRESSION)
annotation class AnnotationWithBinaryRetention

@Retention(AnnotationRetention.RUNTIME)
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

@AnnotationWithRuntimeRetention
class TestRuntime {
    @AnnotationWithRuntimeRetention
    fun baz(@AnnotationWithRuntimeRetention foo : Int) : Int {
        return (<!NOT_SUPPORTED!>@AnnotationWithRuntimeRetention 1<!>)
    }
}

