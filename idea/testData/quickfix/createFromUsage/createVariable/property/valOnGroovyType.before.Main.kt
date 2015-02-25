// "Create property 'foo'" "false"
// ACTION: Convert to expression body
// ACTION: Create extension property 'foo'
// ERROR: Unresolved reference: foo

fun test(): String? {
    return A().<caret>foo
}