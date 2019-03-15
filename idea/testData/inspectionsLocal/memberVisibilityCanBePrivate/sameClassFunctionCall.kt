open class A {
    <caret>internal fun foo() {}

    fun bar(a: A) {
        a.foo()
    }
}
