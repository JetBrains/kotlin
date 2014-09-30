// "Create parameter 'foo'" "false"
// ACTION: Convert to expression body
// ACTION: Create local variable 'foo'
// ACTION: Create property 'foo' from usage
// ERROR: Unresolved reference: foo

trait A {
    val test: Int get() {
        return <caret>foo
    }
}