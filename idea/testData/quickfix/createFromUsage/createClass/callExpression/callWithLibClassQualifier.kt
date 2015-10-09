// "Create class 'Foo'" "false"
// ACTION: Create extension function 'Foo'
// ERROR: Unresolved reference: Foo

fun test() {
    val a = 2.<caret>Foo(1)
}