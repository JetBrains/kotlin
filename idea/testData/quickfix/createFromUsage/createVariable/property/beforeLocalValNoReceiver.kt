// "Create property 'foo' from usage" "false"
// ACTION: Convert to expression body
// ACTION: Disable 'Convert to Expression Body'
// ACTION: Edit intention settings
// ACTION: Create parameter 'foo'
// ACTION: Create local variable 'foo'
// ERROR: Unresolved reference: foo

fun test() {
    fun nestedTest(): Int {
        return <caret>foo
    }
}
