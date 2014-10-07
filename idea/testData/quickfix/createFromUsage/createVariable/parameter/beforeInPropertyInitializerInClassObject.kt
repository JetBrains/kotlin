// "Create parameter 'foo'" "false"
// ACTION: Create property 'foo' from usage
// ERROR: Unresolved reference: foo

class A {
    class object {
        val test: Int = <caret>foo
    }
}