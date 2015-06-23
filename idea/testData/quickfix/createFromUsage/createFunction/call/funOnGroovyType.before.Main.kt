// "Create member function 'foo'" "false"
// ACTION: Convert to expression body
// ACTION: Create extension function 'foo'
// ERROR: Unresolved reference: foo

fun test(): Int {
    return A().<caret>foo()
}