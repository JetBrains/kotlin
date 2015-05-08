// "Create parameter 'foo'" "false"
// ACTION: Create property 'foo'
// ERROR: Unresolved reference: foo

object A {
    val test: Int = <caret>foo
}