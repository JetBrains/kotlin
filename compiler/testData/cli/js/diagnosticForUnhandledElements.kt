package foo

target(AnnotationTarget.CLASSIFIER, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.EXPRESSION)
annotation class fancy

@fancy
class Foo {
    @fancy
    fun baz(@fancy foo : Int) : Int {
        return (@fancy 1)
    }
}
