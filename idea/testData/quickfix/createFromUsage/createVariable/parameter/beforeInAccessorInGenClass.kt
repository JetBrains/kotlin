// "Create parameter 'foo'" "true"

class A<T> {
    val test: T get() {
        return <caret>foo
    }
}