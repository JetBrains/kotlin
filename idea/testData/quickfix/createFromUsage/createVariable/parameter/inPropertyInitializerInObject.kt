// "Create parameter 'foo'" "false"
// ACTION: Create property 'foo'
// ACTION: Rename reference
// ERROR: Unresolved reference: foo

object A {
    val test: Int = <caret>foo
}