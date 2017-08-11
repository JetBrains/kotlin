package test

impl class Foo {
    impl val foo get() = 2
}

fun test(f: Foo) = f.foo