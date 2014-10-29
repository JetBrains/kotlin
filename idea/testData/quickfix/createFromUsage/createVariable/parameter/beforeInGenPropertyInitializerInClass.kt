// "Create parameter 'foo'" "false"
// ERROR: Unresolved reference: foo
// ACTION: Create property 'foo'

class A {
    val <T> test: T = <caret>foo
}