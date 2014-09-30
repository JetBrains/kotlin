// "Create parameter 'foo'" "false"
// ACTION: Convert to expression body
// ACTION: Create local variable 'foo'
// ERROR: Unresolved reference: foo

trait A {
    val test: Int get() {
        return <caret>foo
    }
}