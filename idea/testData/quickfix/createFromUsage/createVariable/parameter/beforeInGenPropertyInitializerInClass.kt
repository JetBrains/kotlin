// "Create parameter 'foo'" "false"
// ERROR: Unresolved reference: foo
// ACTION: Create property 'foo' from usage

class A {
    val <T> test: T = <caret>foo
}