package test

impl class Foo {
    impl val foo get() = 1
}

fun test(f: Foo) = f.foo