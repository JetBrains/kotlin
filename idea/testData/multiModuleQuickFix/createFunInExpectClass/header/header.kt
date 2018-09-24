// "Create member function 'Foo.foo'" "true"
// ERROR: Unresolved reference: foo

expect class Foo

fun test(f: Foo) {
    f.<caret>foo("a", 1)
}