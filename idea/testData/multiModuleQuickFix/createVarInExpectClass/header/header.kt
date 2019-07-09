// "Create member property 'Foo.foo'" "true"

expect class Foo

fun test(f: Foo) {
    f.<caret>foo = 1
}