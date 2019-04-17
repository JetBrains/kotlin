// "Create parameter 'foo'" "false"
// ACTION: Convert to lazy property
// ACTION: Convert property initializer to getter
// ACTION: Create property 'foo'
// ACTION: Rename reference
// ERROR: Unresolved reference: foo

class A {
    companion object {
        val test: Int = <caret>foo
    }
}