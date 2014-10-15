// "Create function 'foo' from usage" "true"
// ERROR: Unresolved reference: foo

fun test(): Int {
    return A().<caret>foo(1, "2")
}