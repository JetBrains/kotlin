// "Create property 'foo' as constructor parameter" "false"
// ACTION: Create member property 'A.foo'
// ACTION: Create extension property 'A.foo'
// ACTION: Rename reference
// ERROR: Unresolved reference: foo

fun test(): String? {
    return A().<caret>foo
}