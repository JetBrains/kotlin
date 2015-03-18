// "Create parameter 'foo'" "false"
// ACTION: Create property 'foo'
// ERROR: Unresolved reference: foo

class A {
    companion object {
        val test: Int = <caret>foo
    }
}