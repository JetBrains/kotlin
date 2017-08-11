package test

impl class Foo {
    impl val <caret>foo get() = 1
}

fun test(f: Foo) = f.foo