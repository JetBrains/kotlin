class Foo<T> {
    @T
    fun foo() = 0
}

class Bar<T : Annotation> {
    @T
    fun foo() = 0
}
