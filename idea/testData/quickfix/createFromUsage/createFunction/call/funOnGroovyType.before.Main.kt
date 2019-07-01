// "Create member function 'foo'" "false"
// ACTION: Create extension function 'A.foo'
// ACTION: Rename reference
// ACTION: Convert to run
// ACTION: Convert to with
// ERROR: Unresolved reference: foo

fun test(): Int {
    return A().<caret>foo()
}