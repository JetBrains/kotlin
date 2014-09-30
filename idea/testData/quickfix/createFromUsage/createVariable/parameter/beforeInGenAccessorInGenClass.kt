// "Create parameter 'foo'" "false"
// ACTION: Convert to expression body
// ACTION: Create local variable 'foo'
// ERROR: Unresolved reference: foo

class A<T> {
    val <T> test: T get() {
        return <caret>foo
    }
}