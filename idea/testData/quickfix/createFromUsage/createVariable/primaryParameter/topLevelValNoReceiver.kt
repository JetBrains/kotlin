// "Create property 'foo' as constructor parameter" "false"
// ACTION: Create property 'foo'
// ACTION: Convert to expression body
// ACTION: Create local variable 'foo'
// ACTION: Create parameter 'foo'
// ERROR: Unresolved reference: foo

fun test(): Int {
    return <caret>foo
}
