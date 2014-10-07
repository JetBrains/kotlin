// "Create parameter 'foo'" "false"
// ACTION: Convert to expression body
// ACTION: Create local variable 'foo'
// ACTION: Create property 'foo' from usage
// ERROR: Unresolved reference: foo

class A<T> {
    val <T> test: T get() {
        return <caret>foo
    }
}