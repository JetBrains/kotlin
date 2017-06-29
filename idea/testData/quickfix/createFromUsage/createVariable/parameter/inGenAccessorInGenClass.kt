// "Create parameter 'foo'" "false"
// ACTION: Create local variable 'foo'
// ACTION: Create property 'foo'
// ACTION: Create extension property 'T.foo'
// ACTION: Rename reference
// ERROR: Unresolved reference: foo

class A<T> {
    val <T> T.test: T get() {
        return <caret>foo
    }
}