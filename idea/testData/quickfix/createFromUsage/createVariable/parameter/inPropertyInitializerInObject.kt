// "Create parameter 'foo'" "false"
// ACTION: Convert property initializer to getter
// ACTION: Create property 'foo'
// ACTION: Rename reference
// ERROR: Unresolved reference: foo

object A {
    val test: Int = <caret>foo
}