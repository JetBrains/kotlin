// "Create parameter 'foo'" "false"
// ERROR: Unresolved reference: foo

class A {
    val <T> test: T = <caret>foo
}