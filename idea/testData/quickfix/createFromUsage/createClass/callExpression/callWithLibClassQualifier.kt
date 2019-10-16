// "Create class 'Foo'" "false"
// ACTION: Create extension function 'Int.Foo'
// ACTION: Rename reference
// ERROR: Unresolved reference: Foo
// WITH_RUNTIME

fun test() {
    val a = 2.<caret>Foo(1)
}