// "Create parameter 'foo'" "false"
// ACTION: Create local variable 'foo'
// ACTION: Create property 'foo'
// ACTION: Rename reference
// ERROR: Unresolved reference: foo

object A {
    val test: Int get() {
        return <caret>foo
    }
}