// "Create class 'Foo'" "false"
// ACTION: Create function 'Foo'
// ACTION: Convert to expression body
// ERROR: Unresolved reference: Foo

fun test(): Int {
    return A().<caret>Foo(1, "2")
}