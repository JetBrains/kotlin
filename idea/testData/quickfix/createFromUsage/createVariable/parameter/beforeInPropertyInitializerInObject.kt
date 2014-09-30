// "Create parameter 'foo'" "false"
// ACTION: Create property 'foo' from usage
// ERROR: Unresolved reference: foo

object A {
    val test: Int = <caret>foo
}