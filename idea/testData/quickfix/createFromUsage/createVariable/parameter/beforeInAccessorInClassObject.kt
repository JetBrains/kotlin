// "Create parameter 'foo'" "false"
// ACTION: Convert to expression body
// ACTION: Create local variable 'foo'
// ERROR: Unresolved reference: foo

class A {
    class object {
        val test: Int get() {
            return <caret>foo
        }
    }
}