// "Create member property 'Foo.foo'" "true"
// ERROR: Unresolved reference: foo

expect class Foo

fun test(f: Foo) {
    f.<caret>foo = 1
}