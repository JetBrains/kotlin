// "Create property 'foo'" "false"
// ACTION: Convert to expression body
// ACTION: Create extension property 'A.foo'
// ACTION: Rename reference
// ERROR: Unresolved reference: foo

fun test(): String? {
    return A().<caret>foo
}