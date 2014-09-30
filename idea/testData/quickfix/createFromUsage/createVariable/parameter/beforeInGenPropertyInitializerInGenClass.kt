// "Create parameter 'foo'" "false"
// ERROR: Unresolved reference: foo

class A<T> {
    val <T> test: T = <caret>foo
}