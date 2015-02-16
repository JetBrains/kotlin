// "Create class 'Foo'" "false"
// ACTION: Create extension function 'Foo'
// ACTION: Replace with infix function call
// ACTION: Split property declaration
// ERROR: Unresolved reference: Foo

fun test() {
    val a = 2.<caret>Foo(1)
}