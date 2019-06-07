// "Create member function 'Foo.foo'" "true"

expect class Foo

fun test(f: Foo) {
    f.<caret>foo("a", 1)
}