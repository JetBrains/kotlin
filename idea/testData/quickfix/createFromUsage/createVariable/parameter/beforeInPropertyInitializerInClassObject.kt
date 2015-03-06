// "Create parameter 'foo'" "false"
// ACTION: Create property 'foo'
// ERROR: Unresolved reference: foo

class A {
    default object {
        val test: Int = <caret>foo
    }
}