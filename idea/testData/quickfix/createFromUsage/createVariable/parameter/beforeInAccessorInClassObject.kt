// "Create parameter 'foo'" "false"
// ACTION: Convert to expression body
// ACTION: Create local variable 'foo'
// ACTION: Create property 'foo'
// ERROR: Unresolved reference: foo

class A {
    default object {
        val test: Int get() {
            return <caret>foo
        }
    }
}