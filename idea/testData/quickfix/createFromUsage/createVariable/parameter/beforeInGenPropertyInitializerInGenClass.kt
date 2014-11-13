// "Create parameter 'foo'" "false"
// ACTION: Create property 'foo'
// ERROR: Unresolved reference: foo

class A<T> {
    val <T> test: T = <caret>foo
}